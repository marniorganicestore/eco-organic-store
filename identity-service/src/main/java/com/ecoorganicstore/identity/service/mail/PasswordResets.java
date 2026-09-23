package com.ecoorganicstore.identity.service.mail;

import com.ecoorganicstore.identity.web.AuthDtos.ConfirmResetRequest;
import com.ecoorganicstore.identity.web.AuthDtos.MessageResponse;
import com.ecoorganicstore.identity.web.AuthDtos.RequestResetRequest;

public interface PasswordResets {
    MessageResponse request(RequestResetRequest request);

    MessageResponse confirm(ConfirmResetRequest request);
}
