package com.skripsi.koma.service.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.skripsi.koma.dto.ApiResponse;
import com.skripsi.koma.dto.approval.ApprovalDTO;
import com.skripsi.koma.dto.facility.FacilityDTO;
import com.skripsi.koma.dto.notification.NotificationDTO;
import com.skripsi.koma.dto.property.PropertyDTO;
import com.skripsi.koma.dto.property.PropertyDetailDTO;
import com.skripsi.koma.dto.property.PropertyPhotoDTO;
import com.skripsi.koma.enums.ApprovalStatus;
import com.skripsi.koma.enums.BillingStatus;
import com.skripsi.koma.enums.StatusCode;
import com.skripsi.koma.model.billing.BillingModel;
import com.skripsi.koma.model.facility.FacilityCategoryModel;
import com.skripsi.koma.model.property.PropertyFacilityModel;
import com.skripsi.koma.model.property.PropertyKeeperModel;
import com.skripsi.koma.model.property.PropertyModel;
import com.skripsi.koma.model.property.PropertyPhotoModel;
import com.skripsi.koma.model.unit.UnitModel;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.repository.facility.FacilityCategoryRepository;
import com.skripsi.koma.repository.property.PropertyFacilityRepository;
import com.skripsi.koma.repository.property.PropertyKeeperRepository;
import com.skripsi.koma.repository.property.PropertyPhotoRepository;
import com.skripsi.koma.repository.property.PropertyRepository;
import com.skripsi.koma.service.notification.NotificationService;
import com.skripsi.koma.service.user.UserService;
import com.skripsi.koma.util.CustomExceptions;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyService {
  private final PropertyRepository propertyRepository;
  private final UserService userService;
  private final PropertyRatingService propertyRatingService;
  private final PropertyPhotoRepository propertyPhotoRepository;
  private final PropertyFacilityRepository propertyFacilityRepository;
  private final FacilityCategoryRepository facilityCategoryRepository;
  private final PropertyKeeperRepository propertyKeeperRepository;
  private final NotificationService notificationService;

  // buat landing page
  public ApiResponse<List<PropertyDTO>> getAllProperty() {
    List<PropertyDTO> propertyDTOs = new ArrayList<>();
    List<PropertyModel> properties = propertyRepository.findAll();

    if (properties == null || properties.isEmpty()) {
      throw new CustomExceptions(StatusCode.NOT_FOUND);
    }
    properties.stream().forEach(property -> {
      PropertyDTO dto = PropertyDTO.mapToDTO(property);
      Map<String, Object> ratingData = propertyRatingService.getRatingSummary(property.getId());
      dto.setRating((Double) ratingData.get("rating"));
      dto.setTotalRater((Integer) ratingData.get("totalRater"));
      propertyDTOs.add(dto);
    });
    return new ApiResponse<>(true, "Property ditemukkan", propertyDTOs);
  }

  // list by pemilik id
  public ApiResponse<List<PropertyDTO>> getAllPropertyByOwnerId() {
    UserModel owner = userService.getCurrentUser();
    List<PropertyDTO> propertyDTOs = new ArrayList<>();
    List<PropertyModel> properties = propertyRepository.findAllByOwnerId(owner.getId());
    if (properties == null || properties.isEmpty()) {
      throw new CustomExceptions(StatusCode.NOT_FOUND);
    }
    properties.stream().forEach(property -> {
      Map<String, Object> ratingData = propertyRatingService.getRatingSummary(property.getId());
      double rating = (double) ratingData.get("rating");
      int totalRater = (int) ratingData.get("totalRater");
      PropertyDTO dto = PropertyDTO.mapToDTO(property);
      propertyDTOs.add(dto);
    });
    return new ApiResponse<>(true, "Property ditemukkan", propertyDTOs);
  }

  public ApiResponse<PropertyDetailDTO> getPropertyDetail(Long propertyId) {
    PropertyModel property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new CustomExceptions(StatusCode.NOT_FOUND));
    return new ApiResponse<>(true, "Property ditemukkan", PropertyDetailDTO.mapToDTO(property));
  }

  @Transactional
  public ApiResponse<PropertyDTO> createProperty(PropertyDTO request) {
    UserModel owner = userService.getCurrentUser();
    PropertyModel property = new PropertyModel();
    BeanUtils.copyProperties(request, property);
    property.setOwner(owner);
    property.setUserCreate(owner.getEmail());
    propertyRepository.save(property);
    request.setId(property.getId());
    return new ApiResponse<>(true, "Property dibuat", PropertyDTO.mapToDTO(property));
  }

  @Transactional
  public ApiResponse<PropertyDTO> updateProperty(Long id, PropertyDTO request) {
    UserModel owner = userService.getCurrentUser();
    PropertyModel property = propertyRepository.findById(id)
        .orElseThrow(
            () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Property " + id + " tidak ditemukan",
                null));
    BeanUtils.copyProperties(request, property, "id");
    property.setUserUpdate(owner.getEmail());
    propertyRepository.save(property);
    return new ApiResponse<>(true, "Property berhasil diperbarui!", PropertyDTO.mapToDTO(property));
  }

  @Transactional
  public ApiResponse<Void> deleteProperty(Long id) {
    if (!propertyRepository.existsById(id)) {
      throw new CustomExceptions(HttpStatus.NOT_FOUND, "Property ID " + id + " tidak ditemukan", null);
    }
    propertyRepository.deleteById(id);
    return new ApiResponse<>(true, "Property dihapus", null);
  }

  @Transactional
  public ApiResponse<List<PropertyPhotoDTO>> uploadPhotos(Long propertyId, List<PropertyPhotoDTO> photoDTOs) {
    PropertyModel property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND, "Property not found", null));
    List<PropertyPhotoDTO> result = new ArrayList<>();
    for (PropertyPhotoDTO photoDTO : photoDTOs) {
      PropertyPhotoModel photoModel = new PropertyPhotoModel();
      BeanUtils.copyProperties(photoDTO, photoModel);
      photoModel.setProperty(property);
      photoModel.setUploader(userService.getCurrentUser());
      propertyPhotoRepository.save(photoModel);
      PropertyPhotoDTO dto = new PropertyPhotoDTO();
      BeanUtils.copyProperties(photoModel, dto);
      dto.setPropertyId(propertyId);
      dto.setPropertyPhotoId(photoModel.getId());
      dto.setUploaderId(photoModel.getUploader() != null ? photoModel.getUploader().getId() : null);
      result.add(dto);
    }
    return new ApiResponse<>(true, "Photos uploaded successfully", result);
  }

  @Transactional
  public ApiResponse<List<FacilityDTO>> addPropertyFacilities(Long propertyId, List<FacilityDTO> facilityDTOs) {
    PropertyModel property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND, "Property not found", null));
    List<FacilityDTO> result = new ArrayList<>();
    for (FacilityDTO dto : facilityDTOs) {
      FacilityCategoryModel category = facilityCategoryRepository.findById(dto.getFacilityCategoryId())
          .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND, "Facility category not found", null));
      PropertyFacilityModel facility = new PropertyFacilityModel();
      facility.setProperty(property);
      facility.setFacilityCategory(category);
      facility.setQuantity(dto.getQuantity());
      propertyFacilityRepository.save(facility);
      result.add(FacilityDTO.mapUnitFacilityToDTO(facility));
    }
    return new ApiResponse<>(true, "Facilities added successfully", result);
  }

  @Transactional
  public ApiResponse<Void> applyAsPropertyKeeper(Long propertyId) {
    PropertyModel property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND, "Property not found", null));
    UserModel user = userService.getCurrentUser();
    // Insert application to property_keeper
    PropertyKeeperModel keeperModel = new PropertyKeeperModel();
    keeperModel.setProperty(property);
    keeperModel.setKeeper(user);
    keeperModel.setApprovalStatus(null); // status null
    keeperModel.setApprover(null); // belum ada approver
    propertyKeeperRepository.save(keeperModel);
    // Send notification to property owner
    NotificationDTO notif = new NotificationDTO();
    notif.setUserId(property.getOwner().getId());
    notif.setPropertyId(property.getId());
    notif.setPropertyKeeperId(keeperModel.getId());
    notif.setNotificationCategory("APPROVAL-KEEPER");
    notif.setContent("Pengajuan sebagai penjaga kos oleh " + user.getName());
    notificationService.createNotification(notif);
    return new ApiResponse<>(true, "Pengajuan sebagai penjaga kos berhasil", null);
  }

  @Transactional
  public ApiResponse<Void> approvalBooking(Long id, ApprovalDTO approvalDTO) {
    ApiResponse<Void> apiResponse = new ApiResponse<>();
    PropertyKeeperModel propertyKeeperModel = propertyKeeperRepository.findById(id).orElse(null);
    if (propertyKeeperModel == null) {
      throw new CustomExceptions(HttpStatus.NOT_FOUND, "Property Keeper tidak ditemukan", null);
    }
    if (approvalDTO.getDecision().equals(ApprovalStatus.APPROVE)) {
      propertyKeeperModel.setApprovalStatus(ApprovalStatus.APPROVE);
      propertyKeeperModel.setApprover(userService.getCurrentUser());
      NotificationDTO notificationRequest = new NotificationDTO();
      notificationRequest.setPropertyId(propertyKeeperModel.getProperty().getId());
      notificationRequest.setPropertyKeeperId(propertyKeeperModel.getId());
      notificationRequest.setUserId(propertyKeeperModel.getKeeper().getId());
      notificationRequest.setNotificationCategory("APPLY-KEEPER-APPROVED");
      notificationRequest.setContent(
          "Pengajuan anda sebagai penjaga kos telah disetujui. Silahkan hubungi pemilik kos untuk informasi lebih lanjut");
      notificationService.createNotification(notificationRequest);
    } else {
      propertyKeeperModel.setApprovalStatus(ApprovalStatus.REJECT);
      propertyKeeperModel.setApprover(userService.getCurrentUser());
      NotificationDTO notificationRequest = new NotificationDTO();
      notificationRequest.setPropertyId(propertyKeeperModel.getProperty().getId());
      notificationRequest.setPropertyKeeperId(propertyKeeperModel.getId());
      notificationRequest.setUserId(propertyKeeperModel.getKeeper().getId());
      notificationRequest.setNotificationCategory("APPLY-KEEPER-REJECTED");
      notificationRequest.setContent(
          "Pengajuan anda sebagai penjaga kos telah ditolak. Silahkan hubungi pemilik kos untuk informasi lebih lanjut");
      notificationService.createNotification(notificationRequest);
    }
    propertyKeeperRepository.save(propertyKeeperModel);
    apiResponse.setSuccess(true);
    apiResponse.setMessage("Approval property keeper berhasil");
    return apiResponse;
  }

}
