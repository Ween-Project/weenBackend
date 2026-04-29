package com.ween.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${ween.mail.from:noreply@ween.az}")
    private String fromEmail;

    @Value("${ween.app.name:Ween}")
    private String appName;

    public void sendVerificationEmail(String to, String fullName, String verificationLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Verify Your Account");
            helper.setText(buildVerificationEmailHtml(fullName, verificationLink), true);

            mailSender.send(mimeMessage);
            log.info("Verification email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(String to, String fullName, String resetLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Change Your Password");
            helper.setText(buildPasswordResetEmailHtml(fullName, resetLink), true);

            mailSender.send(mimeMessage);
            log.info("Password reset email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildVerificationEmailHtml(String fullName, String verificationLink) {
        String safeName = escapeHtml(fullName == null || fullName.isBlank() ? "Volunteer" : fullName);
        String safeLink = escapeHtml(verificationLink);

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\" />" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                "  <title>Ween - Verify Your Account</title>" +
                "  <style>" +
                "    * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "    body { background-color: #f0f4f0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1a1a1a; padding: 40px 16px; }" +
                "    .email-wrapper { max-width: 580px; margin: 0 auto; }" +
                "    .header { background-color: #2d7a3a; border-radius: 16px 16px 0 0; padding: 36px 40px 32px; text-align: center; }" +
                "    .logo { width: 72px; height: 72px; border: 3px solid rgba(255,255,255,0.3); border-radius: 18px; margin-bottom: 16px; object-fit: contain; }" +
                "    .brand-name { font-size: 28px; font-weight: 800; color: #ffffff; letter-spacing: 2px; }" +
                "    .brand-tagline { font-size: 13px; color: rgba(255,255,255,0.75); margin-top: 4px; letter-spacing: 0.5px; }" +
                "    .card { background-color: #ffffff; padding: 44px 40px 36px; }" +
                "    .greeting { font-size: 22px; font-weight: 700; color: #1a1a1a; margin-bottom: 12px; }" +
                "    .greeting span { color: #2d7a3a; }" +
                "    .intro-text { font-size: 15px; color: #444444; line-height: 1.7; margin-bottom: 28px; }" +
                "    .divider { height: 2px; background: linear-gradient(to right, #2d7a3a22, #2d7a3a55, #2d7a3a22); border-radius: 2px; margin-bottom: 28px; }" +
                "    .features-title { font-size: 13px; font-weight: 700; color: #888888; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 16px; }" +
                "    .features { display: flex; flex-direction: column; gap: 12px; margin-bottom: 32px; }" +
                "    .feature-item { display: flex; align-items: flex-start; gap: 12px; }" +
                "    .feature-icon { width: 32px; height: 32px; min-width: 32px; background-color: #eaf5ec; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 15px; }" +
                "    .feature-text { padding-top: 6px; }" +
                "    .feature-text strong { display: block; font-size: 13px; font-weight: 700; color: #1a1a1a; margin-bottom: 1px; }" +
                "    .feature-text span { font-size: 12px; color: #777777; line-height: 1.5; }" +
                "    .cta-wrap { text-align: center; margin: 32px 0 28px; }" +
                "    .cta-btn { display: inline-block; background-color: #2d7a3a; color: #ffffff !important; text-decoration: none; font-size: 16px; font-weight: 700; padding: 16px 48px; border-radius: 50px; letter-spacing: 0.5px; box-shadow: 0 4px 20px rgba(45, 122, 58, 0.35); }" +
                "    .cta-note { font-size: 12px; color: #aaaaaa; margin-top: 12px; }" +
                "    .security-box { background-color: #f7faf7; border: 1px solid #d4e9d7; border-radius: 10px; padding: 16px 20px; margin-top: 8px; margin-bottom: 8px; }" +
                "    .security-box p { font-size: 12px; color: #666666; line-height: 1.6; }" +
                "    .security-box p strong { color: #2d7a3a; }" +
                "    .footer { background-color: #1e5228; border-radius: 0 0 16px 16px; padding: 28px 40px; text-align: center; }" +
                "    .footer-logo { font-size: 18px; font-weight: 900; color: #ffffff; letter-spacing: 2px; margin-bottom: 10px; }" +
                "    .footer-links { font-size: 12px; color: rgba(255,255,255,0.6); margin-bottom: 14px; }" +
                "    .footer-links a { color: #ffffff !important; text-decoration: none; margin: 0 8px; }" +
                "    .footer-copy { font-size: 11px; color: rgba(255,255,255,0.4); }" +
                "    @media (max-width: 480px) {" +
                "      body { padding: 20px 8px; }" +
                "      .header { padding: 28px 24px 24px; border-radius: 12px 12px 0 0; }" +
                "      .card { padding: 32px 24px 28px; }" +
                "      .footer { padding: 22px 24px; border-radius: 0 0 12px 12px; }" +
                "      .cta-btn { font-size: 15px; padding: 14px 36px; }" +
                "      .greeting { font-size: 20px; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"email-wrapper\">" +
                "    <div class=\"header\">" +
                "      <img class=\"logo\" src=\"https://res.cloudinary.com/dyb2pz75u/image/upload/v1776432968/Screenshot_2026-04-17_173547_midzjj.png\" alt=\"Ween Logo\" />" +
                "      <div class=\"brand-name\">WEEN</div>" +
                "      <div class=\"brand-tagline\">Student &amp; Youth Volunteering Platform</div>" +
                "    </div>" +
                "    <div class=\"card\">" +
                "      <p class=\"greeting\">Hey, <span>" + safeName + "</span>! </p>" +
                "      <p class=\"intro-text\">Welcome to the Ween family! Click the button below to activate your account. One verification - a lifetime profile with all opportunities unlocked just for you.</p>" +
                "      <div class=\"divider\"></div>" +
                "      <p class=\"features-title\">Here's what we've prepared for you</p>" +
                "      <div class=\"features\">" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#127919;</div><div class=\"feature-text\"><strong>Project Discovery</strong><span>Explore 300+ local &amp; international volunteering opportunities</span></div></div>" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#128220;</div><div class=\"feature-text\"><strong>Automatic Certificates</strong><span>Attend an event and your certificate is added to your profile instantly</span></div></div>" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#129689;</div><div class=\"feature-text\"><strong>Earn Ween Coins</strong><span>Gain coins for every participation and climb the leaderboard</span></div></div>" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#128279;</div><div class=\"feature-text\"><strong>Your Personal Profile Link</strong><span>ween.az/@you - share your professional volunteering portfolio</span></div></div>" +
                "      </div>" +
                "      <div class=\"cta-wrap\">" +
                "        <a href=\"" + safeLink + "\" class=\"cta-btn\">Verify My Account</a>" +
                "        <p class=\"cta-note\">This link is valid for 24 hours</p>" +
                "      </div>" +
                "      <div class=\"security-box\">" +
                "        <p>&#128274; <strong>Security note:</strong> If you didn't create this account, simply ignore this email. If the button doesn't work, copy this link into your browser:<br /><span style=\"color:#2d7a3a; font-size:11px; word-break: break-all;\">" + safeLink + "</span></p>" +
                "      </div>" +
                "    </div>" +
                "    <div class=\"footer\">" +
                "      <div class=\"footer-logo\">WEEN</div>" +
                "      <div class=\"footer-links\">" +
                "        <a href=\"https://ween.az\">ween.az</a>" +
                "        <a href=\"https://ween.az/privacy\">Privacy</a>" +
                "        <a href=\"https://ween.az/unsubscribe\">Unsubscribe</a>" +
                "      </div>" +
                "      <div class=\"footer-copy\">© 2025 Ween. All rights reserved. Baku, Azerbaijan.</div>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String buildPasswordResetEmailHtml(String fullName, String resetLink) {
        String safeName = escapeHtml(fullName == null || fullName.isBlank() ? "Volunteer" : fullName);
        String safeLink = escapeHtml(resetLink);

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\" />" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                "  <title>Ween - Change Your Password</title>" +
                "  <style>" +
                "    * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "    body { background-color: #f0f4f0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1a1a1a; padding: 40px 16px; }" +
                "    .email-wrapper { max-width: 580px; margin: 0 auto; }" +
                "    .header { background-color: #2d7a3a; border-radius: 16px 16px 0 0; padding: 36px 40px 32px; text-align: center; }" +
                "    .logo { width: 72px; height: 72px; border: 3px solid rgba(255,255,255,0.3); border-radius: 18px; margin-bottom: 16px; object-fit: contain; }" +
                "    .brand-name { font-size: 28px; font-weight: 800; color: #ffffff; letter-spacing: 2px; }" +
                "    .brand-tagline { font-size: 13px; color: rgba(255,255,255,0.75); margin-top: 4px; letter-spacing: 0.5px; }" +
                "    .card { background-color: #ffffff; padding: 44px 40px 36px; }" +
                "    .greeting { font-size: 22px; font-weight: 700; color: #1a1a1a; margin-bottom: 12px; }" +
                "    .greeting span { color: #2d7a3a; }" +
                "    .intro-text { font-size: 15px; color: #444444; line-height: 1.7; margin-bottom: 28px; }" +
                "    .divider { height: 2px; background: linear-gradient(to right, #2d7a3a22, #2d7a3a55, #2d7a3a22); border-radius: 2px; margin-bottom: 28px; }" +
                "    .features-title { font-size: 13px; font-weight: 700; color: #888888; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 16px; }" +
                "    .features { display: flex; flex-direction: column; gap: 12px; margin-bottom: 32px; }" +
                "    .feature-item { display: flex; align-items: flex-start; gap: 12px; }" +
                "    .feature-icon { width: 32px; height: 32px; min-width: 32px; background-color: #eaf5ec; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 15px; }" +
                "    .feature-text { padding-top: 6px; }" +
                "    .feature-text strong { display: block; font-size: 13px; font-weight: 700; color: #1a1a1a; margin-bottom: 1px; }" +
                "    .feature-text span { font-size: 12px; color: #777777; line-height: 1.5; }" +
                "    .cta-wrap { text-align: center; margin: 32px 0 28px; }" +
                "    .cta-btn { display: inline-block; background-color: #2d7a3a; color: #ffffff !important; text-decoration: none; font-size: 16px; font-weight: 700; padding: 16px 48px; border-radius: 50px; letter-spacing: 0.5px; box-shadow: 0 4px 20px rgba(45, 122, 58, 0.35); }" +
                "    .cta-note { font-size: 12px; color: #aaaaaa; margin-top: 12px; }" +
                "    .security-box { background-color: #f7faf7; border: 1px solid #d4e9d7; border-radius: 10px; padding: 16px 20px; margin-top: 8px; margin-bottom: 8px; }" +
                "    .security-box p { font-size: 12px; color: #666666; line-height: 1.6; }" +
                "    .security-box p strong { color: #2d7a3a; }" +
                "    .footer { background-color: #1e5228; border-radius: 0 0 16px 16px; padding: 28px 40px; text-align: center; }" +
                "    .footer-logo { font-size: 18px; font-weight: 900; color: #ffffff; letter-spacing: 2px; margin-bottom: 10px; }" +
                "    .footer-links { font-size: 12px; color: rgba(255,255,255,0.6); margin-bottom: 14px; }" +
                "    .footer-links a { color: #ffffff !important; text-decoration: none; margin: 0 8px; }" +
                "    .footer-copy { font-size: 11px; color: rgba(255,255,255,0.4); }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"email-wrapper\">" +
                "    <div class=\"header\">" +
                "      <img class=\"logo\" src=\"https://res.cloudinary.com/dyb2pz75u/image/upload/v1776432968/Screenshot_2026-04-17_173547_midzjj.png\" alt=\"Ween Logo\" />" +
                "      <div class=\"brand-name\">WEEN</div>" +
                "      <div class=\"brand-tagline\">Student &amp; Youth Volunteering Platform</div>" +
                "    </div>" +
                "    <div class=\"card\">" +
                "      <p class=\"greeting\">Hey, <span>" + safeName + "</span>! </p>" +
                "      <p class=\"intro-text\">You requested to change your password on Ween. Click the button below to set a new password for your account.</p>" +
                "      <div class=\"divider\"></div>" +
                "      <p class=\"features-title\">What happens next?</p>" +
                "      <div class=\"features\">" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#128273;</div><div class=\"feature-text\"><strong>Set New Password</strong><span>Create a strong and secure password for your account</span></div></div>" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#128737;&#65039;</div><div class=\"feature-text\"><strong>Secure Access</strong><span>After changing, you'll be able to log in with your new password</span></div></div>" +
                "        <div class=\"feature-item\"><div class=\"feature-icon\">&#128231;</div><div class=\"feature-text\"><strong>Account Protection</strong><span>This helps keep your volunteering profile and data safe</span></div></div>" +
                "      </div>" +
                "      <div class=\"cta-wrap\">" +
                "        <a href=\"" + safeLink + "\" class=\"cta-btn\">Change My Password</a>" +
                "        <p class=\"cta-note\">This link is valid for 24 hours</p>" +
                "      </div>" +
                "      <div class=\"security-box\">" +
                "        <p>&#128274; <strong>Security note:</strong> If you didn't request a password change, please ignore this email. Someone may have entered your email address by mistake.<br /><br />If the button doesn't work, copy this link into your browser:<br /><span style=\"color:#2d7a3a; font-size:11px; word-break: break-all;\">" + safeLink + "</span></p>" +
                "      </div>" +
                "    </div>" +
                "    <div class=\"footer\">" +
                "      <div class=\"footer-logo\">WEEN</div>" +
                "      <div class=\"footer-links\">" +
                "        <a href=\"https://ween.az\">ween.az</a>" +
                "        <a href=\"https://ween.az/privacy\">Privacy</a>" +
                "        <a href=\"https://ween.az/unsubscribe\">Unsubscribe</a>" +
                "      </div>" +
                "      <div class=\"footer-copy\">© 2025 Ween. All rights reserved. Baku, Azerbaijan.</div>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
