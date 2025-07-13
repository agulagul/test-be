package com.skripsi.koma.repository.property;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skripsi.koma.model.BaseModel;
import com.skripsi.koma.model.property.PropertyModel;
import com.skripsi.koma.model.property.PropertyRatingModel;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.repository.BaseRepository;

public interface PropertyRatingRepository extends BaseRepository<PropertyRatingModel>{
    Optional<PropertyRatingModel> findByPropertyAndOccupant(PropertyModel property, UserModel occupant);
    List<PropertyRatingModel> findByProperty(PropertyModel property);
}
