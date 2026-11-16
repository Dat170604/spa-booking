package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.entity.AppointmentEntity;
import com.spa.spabookingservice.service.EmailNotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // 1. Gửi thông báo đặt lịch thành công
    @Override
    @Async
    public void sendBookingCreatedEmail(AppointmentEntity appointment) {
        String to = appointment.getCustomer().getEmail();
        String subject = "[Lotus Spa] Xác nhận tiếp nhận lịch hẹn #" + appointment.getId();
        String services = getServiceNames(appointment);

        String html = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2e7d32;">Đặt lịch thành công tại Lotus Spa!</h2>
                    <p>Xin chào <b>%s</b>,</p>
                    <p>Chúng tôi đã tiếp nhận yêu cầu đặt lịch của bạn với thông tin chi tiết như sau:</p>
                    <ul>
                        <li><b>Mã lịch hẹn:</b> #%d</li>
                        <li><b>Chi nhánh:</b> %s (%s)</li>
                        <li><b>Dịch vụ:</b> %s</li>
                        <li><b>Thời gian:</b> %s - %s | Ngày %s</li>
                        <li><b>Tổng chi phí dự kiến:</b> %,.0f VNĐ</li>
                        <li><b>Trạng thái hiện tại:</b> Chờ xác nhận (PENDING)</li>
                    </ul>
                    <p><i>Lưu ý: Bạn có thể hủy lịch miễn phí trước giờ hẹn ít nhất 24 giờ trên hệ thống.</i></p>
                    <p>Trân trọng,<br/><b>Đội ngũ Lotus Spa</b></p>
                </div>
                """.formatted(
                appointment.getCustomer().getFullName(),
                appointment.getId(),
                appointment.getBranch().getName(),
                appointment.getBranch().getAddress(),
                services,
                appointment.getStartTime().format(TIME_FMT),
                appointment.getEndTime().format(TIME_FMT),
                appointment.getAppointmentDate().format(DATE_FMT),
                appointment.getTotalAmount()
        );

        sendHtmlEmail(to, subject, html);
    }

    // 2. Gửi thông báo thay đổi trạng thái lịch hẹn (CONFIRMED, COMPLETED, CANCELLED)
    @Override
    @Async
    public void sendStatusChangedEmail(AppointmentEntity appointment, String reason) {
        String to = appointment.getCustomer().getEmail();
        String statusText = switch (appointment.getStatus()) {
            case "CONFIRMED" -> "ĐÃ ĐƯỢC XÁC NHẬN";
            case "COMPLETED" -> "ĐÃ HOÀN THÀNH";
            case "CANCELLED" -> "ĐÃ BỊ HỦY";
            default -> appointment.getStatus();
        };

        String subject = String.format("[Lotus Spa] Lịch hẹn #%d của bạn %s", appointment.getId(), statusText);
        String reasonBlock = (reason != null && !reason.isBlank())
                ? "<p><b>Ghi chú / Lý do:</b> " + reason + "</p>"
                : "";

        String html = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #1565c0;">Cập nhật trạng thái lịch hẹn #%d</h2>
                    <p>Xin chào <b>%s</b>,</p>
                    <p>Lịch hẹn của bạn vào lúc <b>%s ngày %s</b> tại chi nhánh <b>%s</b> đã chuyển sang trạng thái: <b>%s</b>.</p>
                    %s
                    <p>Cảm ơn bạn đã tin tưởng dịch vụ của Lotus Spa!</p>
                </div>
                """.formatted(
                appointment.getId(),
                appointment.getCustomer().getFullName(),
                appointment.getStartTime().format(TIME_FMT),
                appointment.getAppointmentDate().format(DATE_FMT),
                appointment.getBranch().getName(),
                statusText,
                reasonBlock
        );

        sendHtmlEmail(to, subject, html);
    }

    // 3. Gửi thông báo nhắc lịch sắp diễn ra
    @Override
    @Async
    public void sendAppointmentReminderEmail(AppointmentEntity appointment) {
        String to = appointment.getCustomer().getEmail();
        String subject = "[Lotus Spa] Nhắc hẹn: Bạn có lịch chăm sóc lúc " + appointment.getStartTime().format(TIME_FMT) + " hôm nay";

        String html = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #ef6c00;">Nhắc nhở lịch hẹn sắp tới</h2>
                    <p>Xin chào <b>%s</b>,</p>
                    <p>Lotus Spa xin nhắc bạn có lịch hẹn dịch vụ <b>%s</b> vào lúc <b>%s hôm nay (%s)</b>.</p>
                    <p><b>Địa điểm:</b> %s - %s</p>
                    <p>Vui lòng đến trước 10 phút để được phục vụ chu đáo nhất nhé!</p>
                </div>
                """.formatted(
                appointment.getCustomer().getFullName(),
                getServiceNames(appointment),
                appointment.getStartTime().format(TIME_FMT),
                appointment.getAppointmentDate().format(DATE_FMT),
                appointment.getBranch().getName(),
                appointment.getBranch().getAddress()
        );

        sendHtmlEmail(to, subject, html);
    }

    private String getServiceNames(AppointmentEntity appointment) {
        return appointment.getDetails().stream()
                .map(d -> d.getService().getName())
                .collect(Collectors.joining(", "));
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email thông báo tới {}: {}", to, subject);
        } catch (MessagingException e) {
            // Bắt lỗi log lại chứ không throw exception làm ảnh hưởng luồng chính
            log.error("Lỗi khi gửi email tới {}: {}", to, e.getMessage());
        }
    }
}