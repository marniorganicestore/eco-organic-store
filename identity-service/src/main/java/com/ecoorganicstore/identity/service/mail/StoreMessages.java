package com.ecoorganicstore.identity.service.mail;

import java.util.List;
import java.util.Locale;

public final class StoreMessages {
    private final String storeName;
    private final String fromAddress;
    private final String adminAddress;
    private final String storefrontUrl;

    public StoreMessages(String storeName, String fromAddress, String adminAddress, String storefrontUrl) {
        this.storeName = blankTo(storeName, "Marni eco organic store");
        this.fromAddress = blankTo(fromAddress, "admin@eco-organic-store.com");
        this.adminAddress = blankTo(adminAddress, this.fromAddress);
        this.storefrontUrl = stripTrailingSlash(blankTo(storefrontUrl, "http://localhost:5173"));
    }

    public String storefrontUrl() {
        return storefrontUrl;
    }

    public String fromAddress() {
        return fromAddress;
    }

    public OutboundMail welcome(String name, String email) {
        String greeting = greeting(name);
        String text = """
                %s

                Your account at %s is ready. Sign in with %s whenever you want to shop.

                %s
                """.formatted(greeting, storeName, email, footer());
        String html = page(greeting, """
                <p style="margin:0 0 16px;">Your account at %s is ready. Sign in with <strong>%s</strong> whenever you want to shop.</p>
                %s
                """.formatted(esc(storeName), esc(email), button(storefrontUrl + "/shop", "Visit the shop")));
        return new OutboundMail(email, null, "Welcome to " + storeName, text.strip(), html);
    }

    public OutboundMail passwordReset(String name, String email, String resetUrl) {
        String greeting = greeting(name);
        String text = """
                %s

                We received a request to reset the password for %s.
                This link expires in 30 minutes:
                %s

                If you did not ask for this, ignore the message. Your password stays the same.

                %s
                """.formatted(greeting, email, resetUrl, footer());
        String html = page(greeting, """
                <p style="margin:0 0 16px;">We received a request to reset the password for <strong>%s</strong>. The link expires in 30 minutes.</p>
                %s
                <p style="margin:16px 0 0;color:#475569;">If you did not ask for this, ignore the message. Your password stays the same.</p>
                """.formatted(esc(email), button(resetUrl, "Choose a new password")));
        return new OutboundMail(email, null, "Reset your " + storeName + " password", text.strip(), html);
    }

    public OutboundMail passwordChanged(String name, String email) {
        String greeting = greeting(name);
        String text = """
                %s

                The password for %s was just changed. Other signed-in devices were signed out.
                If this was not you, reset the password again from the sign-in page.

                %s
                """.formatted(greeting, email, footer());
        String html = page(greeting, """
                <p style="margin:0 0 16px;">The password for <strong>%s</strong> was just changed. Other signed-in devices were signed out.</p>
                <p style="margin:0;color:#475569;">If this was not you, reset the password again from the sign-in page.</p>
                """.formatted(esc(email)));
        return new OutboundMail(email, null, "Your " + storeName + " password was changed", text.strip(), html);
    }

    public OutboundMail googleSignIn(String name, String email) {
        String greeting = greeting(name);
        String text = """
                %s

                Someone asked to reset the password for %s. This account signs in with Google, so there is no password to reset.
                Use Continue with Google on the sign-in page.

                %s
                """.formatted(greeting, email, footer());
        String html = page(greeting, """
                <p style="margin:0 0 16px;">Someone asked to reset the password for <strong>%s</strong>. This account signs in with Google, so there is no password to reset.</p>
                %s
                """.formatted(esc(email), button(storefrontUrl + "/login", "Sign in with Google")));
        return new OutboundMail(email, null, "Sign in to " + storeName + " with Google", text.strip(), html);
    }

    public OutboundMail order(OrderMailCommand command) {
        String status = command.orderStatus() == null ? "" : command.orderStatus().trim().toUpperCase(Locale.ROOT);
        String headline = headline(status, command.orderNumber());
        String lines = textLines(command.lines());
        String address = command.shippingAddress() == null ? "" : command.shippingAddress().trim();
        String customer = command.customerEmail() == null ? "" : command.customerEmail().trim();
        boolean toCustomer = command.customerOptIn() && !customer.isBlank();
        String to = toCustomer ? customer : adminAddress;
        String bcc = toCustomer && !to.equalsIgnoreCase(adminAddress) ? adminAddress : null;
        String subject = (toCustomer ? "" : "[Desk] ") + headline;
        String greeting = toCustomer ? greeting(command.customerName()) : "Hello,";
        String text = """
                %s

                %s
                %s
                Total %s
                Deliver to: %s

                %s
                """.formatted(greeting, headline, lines, inr(command.totalPaise()), address.isBlank() ? "—" : address, footer());
        String html = page(greeting, """
                <p style="margin:0 0 16px;">%s</p>
                %s
                <p style="margin:16px 0 0;"><strong>Total %s</strong></p>
                <p style="margin:8px 0 0;color:#475569;">Deliver to<br>%s</p>
                """.formatted(
                esc(headline),
                htmlLines(command.lines()),
                esc(inr(command.totalPaise())),
                address.isBlank() ? "—" : esc(address).replace("\n", "<br>")));
        return new OutboundMail(to, bcc, subject, text.strip(), html);
    }

    public static String inr(long paise) {
        boolean negative = paise < 0;
        long abs = Math.abs(paise);
        String formatted = "₹" + String.format(Locale.ENGLISH, "%,d.%02d", abs / 100, abs % 100);
        return negative ? "-" + formatted : formatted;
    }

    private String headline(String status, String orderNumber) {
        String number = orderNumber == null ? "" : orderNumber;
        return switch (status) {
            case "CONFIRMED" -> "Order " + number + " is confirmed";
            case "PACKED" -> "Order " + number + " is packed";
            case "SHIPPED" -> "Order " + number + " is on the way";
            case "DELIVERED" -> "Order " + number + " was delivered";
            default -> "Order " + number + " was updated";
        };
    }

    private String textLines(List<OrderMailCommand.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        for (OrderMailCommand.Line line : lines) {
            String name = line.name() == null ? "Item" : line.name();
            body.append(name).append(" × ").append(line.qty()).append(" · ").append(inr(line.pricePaise())).append('\n');
        }
        return body.toString().strip();
    }

    private String htmlLines(List<OrderMailCommand.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        rows.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:collapse;\">");
        for (OrderMailCommand.Line line : lines) {
            String name = line.name() == null ? "Item" : line.name();
            rows.append("<tr><td style=\"padding:6px 0;border-bottom:1px solid #d1fae5;\">")
                    .append(esc(name))
                    .append(" × ")
                    .append(line.qty())
                    .append("</td><td style=\"padding:6px 0;border-bottom:1px solid #d1fae5;text-align:right;\">")
                    .append(esc(inr(line.pricePaise())))
                    .append("</td></tr>");
        }
        rows.append("</table>");
        return rows.toString();
    }

    private String page(String greeting, String body) {
        String logo = storefrontUrl + "/brand/logo-email.png";
        return """
                <div style="margin:0;padding:32px 16px;background:#f3efe6;font-family:Georgia,'Times New Roman',serif;color:#243528;">
                  <div style="max-width:560px;margin:0 auto;background:#faf4e8;border:1px solid #e7e0d2;border-radius:20px;padding:32px 28px;">
                    <img src="%s" alt="%s" width="248" height="88" style="display:block;width:248px;height:auto;border:0;margin:0 0 22px;" />
                    <h1 style="margin:0 0 16px;font-size:26px;font-weight:600;letter-spacing:-0.02em;color:#243528;">%s</h1>
                    <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif;font-size:15px;line-height:1.6;color:#1e293b;">%s</div>
                    <p style="margin:28px 0 0;padding-top:16px;border-top:1px solid #e7e0d2;font-family:'Segoe UI',Helvetica,Arial,sans-serif;font-size:12px;line-height:1.5;color:#64748b;">%s<br>%s</p>
                  </div>
                </div>
                """.formatted(esc(logo), esc(storeName), esc(greeting), body, esc(storeName), esc(fromAddress));
    }

    private String button(String href, String label) {
        return """
                <p style="margin:0;"><a href="%s" style="display:inline-block;background:#047857;color:#ffffff;text-decoration:none;border-radius:8px;padding:12px 18px;font-family:Segoe UI,sans-serif;">%s</a></p>
                """.formatted(esc(href), esc(label));
    }

    private String footer() {
        return storeName + "\n" + fromAddress;
    }

    private static String greeting(String name) {
        String clean = name == null ? "" : name.trim();
        return clean.isBlank() ? "Hello," : "Hello " + clean + ",";
    }

    static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
