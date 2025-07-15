package com.skripsi.koma.service.unit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skripsi.koma.dto.ApiResponse;
import com.skripsi.koma.dto.facility.FacilityDTO;
import com.skripsi.koma.dto.unit.UnitDetailDTO;
import com.skripsi.koma.model.facility.FacilityCategoryModel;
import com.skripsi.koma.model.property.PropertyModel;
import com.skripsi.koma.model.unit.UnitFacilityModel;
import com.skripsi.koma.model.unit.UnitModel;
import com.skripsi.koma.model.unit.UnitReferrenceModel;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.repository.facility.FacilityCategoryRepository;
import com.skripsi.koma.repository.property.PropertyRepository;
import com.skripsi.koma.repository.unit.UnitReferrenceRepository;
import com.skripsi.koma.repository.unit.UnitRepository;
import com.skripsi.koma.repository.user.UserRepository;
import com.skripsi.koma.model.unit.UnitPhotoModel;
import com.skripsi.koma.repository.unit.UnitPhotoRepository;
import com.skripsi.koma.service.user.UserService;
import com.skripsi.koma.util.CustomExceptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UnitService {
  private final UnitRepository unitRepository;
  private final UserService userService;
  private final PropertyRepository propertyRepository;
  private final FacilityCategoryRepository facilityCategoryRepository;
  private final UnitReferrenceRepository unitReferrenceRepository;
  private final UnitPhotoRepository unitPhotoRepository;
  private static final Logger logger = LoggerFactory.getLogger(UnitService.class);

  public ApiResponse<List<UnitDetailDTO>> getAllUnit() {
    List<UnitModel> unitList = unitRepository.findAll();
    List<UnitDetailDTO> dtoList = new ArrayList<>();
    for (UnitModel unit : unitList) {
      dtoList.add(UnitDetailDTO.mapToDTO(unit));
    }
    return new ApiResponse<>(true, "Daftar kamar berhasil diambil", dtoList);
  }

  public ApiResponse<UnitDetailDTO> getUnitById(Long id, String ref) {
    UnitModel unit = unitRepository.findById(id)
        .orElseThrow(
            () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Kamar dengan ID " + id + " tidak ditemukan", null));
    return new ApiResponse<>(true, "Kamar berhasil ditemukan", UnitDetailDTO.mapToDTO(unit));
  }

  public ApiResponse<UnitDetailDTO> createUnit(UnitDetailDTO request) {
    UserModel user = userService.getCurrentUser();
    Optional<PropertyModel> property = propertyRepository.findById(request.getPropertyId());
    if (property.isEmpty()) {
      logger.error("Invalid Kos ID: {}", request.getPropertyId());
      throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Kos ID Invalid", null);
    }
    UnitModel unit = new UnitModel();
    BeanUtils.copyProperties(request, unit);
    unit.setProperty(property.get());
    List<UnitFacilityModel> facilities = new ArrayList<>();
    if (request.getFacilities() != null) {
      for (FacilityDTO fasilitasDTO : request.getFacilities()) {
        FacilityCategoryModel facilityCategory = facilityCategoryRepository.findById(fasilitasDTO.getFacilityCategoryId())
            .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND,
                "Fasilitas dengan ID " + fasilitasDTO.getFacilityCategoryId() + " tidak ditemukan", null));
        UnitFacilityModel unitFacility = new UnitFacilityModel();
        unitFacility.setFacilityCategory(facilityCategory);
        unitFacility.setQuantity(fasilitasDTO.getQuantity());
        unitFacility.setNotes(fasilitasDTO.getNotes());
        unitFacility.setUnit(unit);
        facilities.add(unitFacility);
      }
      unit.setFacilities(facilities);
    }
    // Handle unit photos
    List<UnitPhotoModel> photoModels = new ArrayList<>();
    if (request.getPhotos() != null) {
      for (var photoDTO : request.getPhotos()) {
        UnitPhotoModel photoModel = new UnitPhotoModel();
        BeanUtils.copyProperties(photoDTO, photoModel);
        photoModel.setUnit(unit);
        photoModel.setUploader(user);
        photoModels.add(photoModel);
      }
      unit.setPhotos(photoModels);
    }
    unitRepository.save(unit);
    if (!photoModels.isEmpty()) {
      unitPhotoRepository.saveAll(photoModels);
    }
    return new ApiResponse<>(true, "Kamar berhasil dibuat!", UnitDetailDTO.mapToDTO(unit));
  }

  public ApiResponse generateReferralLink(Long unitId) {
      UserModel referrer = userService.getUser();
      UnitModel kamar = unitRepository.findById(unitId).orElseThrow();

      String code = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

      UnitReferrenceModel referral = new UnitReferrenceModel();
      referral.setReferrerCode(code);
      referral.setReferrerId(referrer);
      referral.setUnitId(kamar);
      referral.setIsValid(true);
      unitReferrenceRepository.save(referral);

      return new ApiResponse(true, "link berhasil digenerate!", "room-detail/" + unitId + "?ref=" + code);
  }

  public ApiResponse<UnitDetailDTO> updateUnit(Long id, UnitDetailDTO request) {
    UserModel user = userService.getCurrentUser();
    UnitModel unit = unitRepository.findById(id)
        .orElseThrow(
            () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Kamar dengan ID " + id + " tidak ditemukan", null));
    updateUnitFromDTO(unit, request);
    // Handle unit photos update
    if (request.getPhotos() != null) {
      List<UnitPhotoModel> photoModels = new ArrayList<>();
      for (var photoDTO : request.getPhotos()) {
        UnitPhotoModel photoModel = new UnitPhotoModel();
        BeanUtils.copyProperties(photoDTO, photoModel);
        photoModel.setUnit(unit);
        photoModel.setUploader(user);
        photoModels.add(photoModel);
      }
      unit.setPhotos(photoModels);
      unitPhotoRepository.saveAll(photoModels);
    }
    // Handle unit facilities update
    if (request.getFacilities() != null) {
      List<UnitFacilityModel> updatedFacilities = new ArrayList<>();
      List<Long> payloadFacilityIds = new ArrayList<>();
      for (FacilityDTO fasilitasDTO : request.getFacilities()) {
        if (fasilitasDTO.getFacilityId() != null) {
          payloadFacilityIds.add(fasilitasDTO.getFacilityId());
        }
      }
      // Remove facilities not in payload (by id)
      if (unit.getFacilities() != null) {
        unit.getFacilities().removeIf(uf -> uf.getId() != null && !payloadFacilityIds.contains(uf.getId()));
      }
      // Update/insert facilities from payload
      for (FacilityDTO fasilitasDTO : request.getFacilities()) {
        FacilityCategoryModel facilityCategory = facilityCategoryRepository.findById(fasilitasDTO.getFacilityCategoryId())
            .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND,
                "Fasilitas dengan ID " + fasilitasDTO.getFacilityCategoryId() + " tidak ditemukan", null));
        UnitFacilityModel unitFacility = null;
        if (fasilitasDTO.getFacilityId() != null && unit.getFacilities() != null) {
          unitFacility = unit.getFacilities().stream()
            .filter(uf -> uf.getId() != null && uf.getId().equals(fasilitasDTO.getFacilityId()))
            .findFirst().orElse(null);
        }
        if (unitFacility != null) {
          // Update existing
          unitFacility.setQuantity(fasilitasDTO.getQuantity());
          unitFacility.setNotes(fasilitasDTO.getNotes());
          updatedFacilities.add(unitFacility);
        } else {
          // Insert new
          unitFacility = new UnitFacilityModel();
          unitFacility.setUnit(unit);
          unitFacility.setFacilityCategory(facilityCategory);
          unitFacility.setQuantity(fasilitasDTO.getQuantity());
          unitFacility.setNotes(fasilitasDTO.getNotes());
          updatedFacilities.add(unitFacility);
        }
      }
      unit.setFacilities(updatedFacilities);
    }
    unitRepository.save(unit);
    return new ApiResponse<>(true, "unit berhasil diperbarui!", UnitDetailDTO.mapToDTO(unit));
  }

  private void updateUnitFromDTO(UnitModel unit, UnitDetailDTO request) {
    BeanUtils.copyProperties(request, unit, "id");

    if (request.getFacilities() != null) {
      List<UnitFacilityModel> existingFasilitas = unit.getFacilities();
      List<UnitFacilityModel> updatedFasilitas = new ArrayList<>();

      // Map existing fasilitas by fasilitasId for quick lookup
      Map<Long, UnitFacilityModel> existingMap = new HashMap<>();
      if (existingFasilitas != null) {
        for (UnitFacilityModel unitFacilityModel : existingFasilitas) {
          if (unitFacilityModel.getFacilityCategory() != null && unitFacilityModel.getFacilityCategory().getId() != null) {
            existingMap.put(unitFacilityModel.getFacilityCategory().getId(), unitFacilityModel);
          }
        }
      }

      for (FacilityDTO fasilitasDTO : request.getFacilities()) {
        FacilityCategoryModel facilityCategory = facilityCategoryRepository.findById(fasilitasDTO.getFacilityCategoryId())
            .orElseThrow(() -> new CustomExceptions(HttpStatus.NOT_FOUND,
                "Fasilitas dengan ID " + fasilitasDTO.getFacilityCategoryId() + " tidak ditemukan", null));

        UnitFacilityModel unitFasilitas = existingMap.get(fasilitasDTO.getFacilityCategoryId());
        if (unitFasilitas == null) {
          unitFasilitas = new UnitFacilityModel();
          unitFasilitas.setUnit(unit);
          unitFasilitas.setFacilityCategory(facilityCategory);
        }
        updatedFasilitas.add(unitFasilitas);
      }
      unit.setFacilities(updatedFasilitas);
    }
  }

  public ApiResponse<Void> deleteUnit(Long id) {
    if (!unitRepository.existsById(id)) {
      throw new CustomExceptions(HttpStatus.NOT_FOUND, "Kamar dengan ID " + id + " tidak ditemukan", null);
    }
    unitRepository.deleteById(id);
    return new ApiResponse<>(true, "Kamar berhasil dihapus", null);
  }
}
