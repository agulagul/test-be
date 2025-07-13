package com.skripsi.koma.service.notification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.skripsi.koma.dto.ApiResponse;
import com.skripsi.koma.dto.notification.NotificationDTO;
import com.skripsi.koma.enums.BillingStatus;
import com.skripsi.koma.model.billing.BillingModel;
import com.skripsi.koma.model.notification.NotificationModel;
import com.skripsi.koma.model.property.PropertyModel;
import com.skripsi.koma.model.unit.UnitModel;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.repository.billing.BillingRepository;
import com.skripsi.koma.repository.notification.NotificationRepository;
import com.skripsi.koma.repository.property.PropertyRepository;
import com.skripsi.koma.repository.unit.UnitRepository;
import com.skripsi.koma.service.user.UserService;
import com.skripsi.koma.util.CustomExceptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final BillingRepository billingRepository;
    private final UserService userService;

    public ApiResponse<List<NotificationDTO>> getAllByPropertyId(Long propertyId) {
        List<NotificationModel> notificationList = notificationRepository.findByPropertyId(propertyId);
        List<NotificationDTO> dtoList = new ArrayList<>();
        for (NotificationModel notification : notificationList) {
            dtoList.add(NotificationDTO.mapToDTO(notification));
        }
        return new ApiResponse<>(true, "Daftar notifikasi berhasil diambil", dtoList);
    }

    public ApiResponse<List<NotificationDTO>> getAllByUserId() {
        UserModel user = userService.getCurrentUser();
        List<NotificationModel> notificationList = notificationRepository.findByUserId(user.getId());
        if (notificationList.isEmpty()) {
            throw new CustomExceptions(HttpStatus.NOT_FOUND, "Notifikasi tidak ditemukan", null);
        }
        List<NotificationDTO> dtoList = new ArrayList<>();
        for (NotificationModel notification : notificationList) {
            dtoList.add(NotificationDTO.mapToDTO(notification));
        }
        return new ApiResponse<>(true, "Daftar notifikasi berhasil diambil", dtoList);
    }

    public ApiResponse<NotificationDTO> getNotificationDetail(Long id) {
        NotificationModel notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND,
                        "Notifikasi dengan ID " + id + " tidak ditemukan", null));
        return new ApiResponse<>(true, "Notifikasi berhasil ditemukan", NotificationDTO.mapToDTO(notification));
    }

    public ApiResponse<NotificationDTO> createNotification(NotificationDTO notificationRequest) {
        NotificationModel notification = new NotificationModel();
        UserModel user = userService.getCurrentUser();
        UserModel recipient = userService.getUserById(notificationRequest.getUserId());
        notification.setUser(recipient);
        if(notificationRequest.getPropertyId() != null) {
            PropertyModel property = propertyRepository.findById(notificationRequest.getPropertyId()).orElse(null);
            notification.setProperty(property);
        }
        if(notificationRequest.getUnitId() != null) {
            UnitModel unit = unitRepository.findById(notificationRequest.getUnitId()).orElse(null);
            notification.setUnit(unit);
        }
        if(notificationRequest.getBillingId() != null) {
            BillingModel billingModel = billingRepository.findById(notificationRequest.getBillingId()).orElse(null);
            notification.setBilling(billingModel);
        }
        notification.setNotificationCategory(notificationRequest.getNotificationCategory());
        notification.setContent(notificationRequest.getContent());
        if(user!=null){
          notification.setUserCreate(user.getEmail());
        }

        NotificationModel savedNotification = notificationRepository.save(notification);
        return new ApiResponse<>(true, "Notifikasi berhasil dibuat", NotificationDTO.mapToDTO(savedNotification));
    }

    public ApiResponse<NotificationDTO> updateNotification(Long id, NotificationDTO notificationRequest) {
        UserModel user = userService.getCurrentUser();
        NotificationModel notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND,
                        "Notifikasi dengan ID " + id + " tidak ditemukan", null));
        if(notificationRequest.getPropertyId() != null) {
            PropertyModel property = propertyRepository.findById(notificationRequest.getPropertyId()).orElse(null);
            notification.setProperty(property);
        }
        if(notificationRequest.getUnitId() != null) {
            UnitModel unit = unitRepository.findById(notificationRequest.getUnitId()).orElse(null);
            notification.setUnit(unit);
        }
        if(notificationRequest.getBillingId() != null) {
            BillingModel billingModel = billingRepository.findById(notificationRequest.getBillingId()).orElse(null);
            notification.setBilling(billingModel);
        }
        notification.setNotificationCategory(notificationRequest.getNotificationCategory());
        notification.setContent(notificationRequest.getContent());
        notification.setUserUpdate(user.getEmail());

        NotificationModel updatedNotification = notificationRepository.save(notification);
        return new ApiResponse<>(true, "Notifikasi berhasil diperbarui", NotificationDTO.mapToDTO(updatedNotification));
    }

    public ApiResponse<Void> deleteNotification(Long id) {
        NotificationModel notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND,
                        "Notifikasi dengan ID " + id + " tidak ditemukan", null));
        notificationRepository.delete(notification);
        return new ApiResponse<>(true, "Notifikasi berhasil dihapus", null);
    }

    @Scheduled(cron = "0 0 9 * * ?") // Set to run every day at 09:00 AM
    public ApiResponse<Void> sendPendingBillReminder() {
        LocalDate next7days = LocalDate.now().plusDays(7);
        List<BillingModel> pendingBillList = billingRepository.findAllByStatusBillingAndDueDate(BillingStatus.PENDING_PAYMENT, next7days);
        for (BillingModel billing : pendingBillList) {
            NotificationDTO notificationRequest = new NotificationDTO();
            notificationRequest.setBillingId(billing.getId());
            notificationRequest.setUserId(billing.getOccupant().getId());
            notificationRequest.setNotificationCategory("BILLING");
            notificationRequest.setContent("Tagihan Anda untuk pembayaran " + billing.getBillingType() + " jatuh tempo dalam waktu 7 hari.");
            createNotification(notificationRequest);
        }
        return new ApiResponse<>(true, "Notifikasi berhasil dikirim", null);
    }

}
