package com.skripsi.koma.repository.unit;

import java.util.Optional;

import com.skripsi.koma.model.unit.UnitReferrenceModel;
import com.skripsi.koma.repository.BaseRepository;

public interface UnitReferrenceRepository extends BaseRepository<UnitReferrenceModel>{

    UnitReferrenceModel findByReferrerCode(String ref);
    
}
