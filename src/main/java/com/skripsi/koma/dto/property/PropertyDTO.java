package com.skripsi.koma.dto.property;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skripsi.koma.model.property.PropertyModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class PropertyDTO {
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long id;
  
  @JsonProperty(value = "property_name")
  private String propertyName;

  @JsonProperty(value = "address")
  private String address;

  @JsonProperty(value = "city")
  private String city;

  @JsonProperty(value = "latitude")
  private Double latitude;

  @JsonProperty(value = "longitude")
  private Double longitude;

  @JsonProperty(value ="property_type" )
  private String propertyType;

  @JsonProperty(value = "thumbnail_photo_path")
  private String thumbnailPhotoPath;

  @JsonProperty(value = "description")
  private String description;

  @JsonProperty(value = "rating", access = JsonProperty.Access.READ_ONLY)
  private Double rating;

  @JsonProperty(value = "total_rater", access = JsonProperty.Access.READ_ONLY)
  private Integer totalRater;
  
  public static PropertyDTO mapToDTO(PropertyModel property) {
    if (property == null) {
      return null;
    }
    PropertyDTO dto = new PropertyDTO();
    BeanUtils.copyProperties(property, dto);
    return dto;
  }
}
