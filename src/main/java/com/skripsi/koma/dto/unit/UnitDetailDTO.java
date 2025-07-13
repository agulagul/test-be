package com.skripsi.koma.dto.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skripsi.koma.dto.facility.FacilityDTO;
import com.skripsi.koma.model.unit.UnitFacilityModel;
import com.skripsi.koma.model.unit.UnitModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class UnitDetailDTO extends UnitDTO {
  @JsonProperty(value = "photos")
  private List<UnitPhotoDTO> photos;

  @JsonProperty(value = "facilities")
  private List<FacilityDTO> facilities;

  public static UnitDetailDTO mapToDTO(UnitModel unit) {
    UnitDetailDTO dto = new UnitDetailDTO();
    BeanUtils.copyProperties(unit, dto);
    dto.setPropertyId(unit.getProperty().getId());
    if(dto.getOccupantId() != null){
      dto.setOccupantId(unit.getOccupant().getId());
    }

    List<UnitPhotoDTO> photoDTOs = null;
    if (unit.getPhotos() != null) {
      photoDTOs = unit.getPhotos().stream()
          .map(photo -> UnitPhotoDTO.mapToDTO(photo))
          .collect(Collectors.toList());
    }
    dto.setPhotos(photoDTOs);

    if (unit.getFacilities() != null) {
      List<FacilityDTO> facilityDTOs = new ArrayList<>();
      for (UnitFacilityModel unitFacilityModel : unit.getFacilities()) {
        FacilityDTO facilityDTO = new FacilityDTO();
        facilityDTO.setFacilityId(unitFacilityModel.getId());
        facilityDTO.setFacilityCategory(unitFacilityModel.getFacilityCategory().getCategoryName());
        facilityDTO.setFacilityName(unitFacilityModel.getFacilityCategory().getFacilityName());
        facilityDTO.setQuantity(unitFacilityModel.getQuantity());
        facilityDTO.setNotes(unitFacilityModel.getNotes());
        facilityDTOs.add(facilityDTO);
      }
      dto.setFacilities(facilityDTOs);
    }
    return dto;
  }
}
