package com.harvest.identity.config;

import com.harvest.identity.domain.Address;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Earlier profiles stored addresses as plain strings. Reading them as structured
 * addresses keeps login working for those documents. The id is derived from the
 * text so repeated reads stay stable until the customer saves the address.
 */
@ReadingConverter
public class LegacyAddressReadConverter implements Converter<String, Address> {
    @Override
    public Address convert(String source) {
        Address address = new Address();
        String text = source == null ? "" : source.trim();
        address.setId(UUID.nameUUIDFromBytes(("legacy-address:" + text).getBytes(StandardCharsets.UTF_8)).toString());
        address.setLabel("Saved");
        address.setRecipient("");
        address.setLine1(text);
        address.setLine2("");
        address.setCity("");
        address.setState("");
        address.setPostalCode("");
        address.setPhone("");
        address.setDefaultAddress(false);
        return address;
    }
}
