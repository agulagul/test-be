package com.skripsi.koma.repository.unit;

import com.skripsi.koma.model.unit.UnitModel;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.repository.BaseRepository;

public interface UnitRepository extends BaseRepository<UnitModel> {

  UnitModel findByOccupant(UserModel occupant);
}
