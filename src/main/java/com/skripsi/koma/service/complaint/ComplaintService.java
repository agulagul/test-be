package com.skripsi.koma.service.complaint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.skripsi.koma.dto.ApiResponse;
import com.skripsi.koma.dto.complaint.ComplaintDTO;
import com.skripsi.koma.enums.ComplaintStatus;
import com.skripsi.koma.model.complaint.ComplaintModel;
import com.skripsi.koma.model.complaint.ComplaintPhotoModel;
import com.skripsi.koma.model.unit.UnitModel;
import com.skripsi.koma.model.user.UserModel;
import com.skripsi.koma.repository.complaint.ComplaintRepository;
import com.skripsi.koma.repository.unit.UnitRepository;
import com.skripsi.koma.service.user.UserService;
import com.skripsi.koma.util.CustomExceptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UnitRepository unitRepository;
    private final UserService userService;

    public ApiResponse<List<ComplaintDTO>> getAllComplaintByProperty(Long propertyId) {
        UserModel currentUser = userService.getCurrentUser();
        String userRole = currentUser.getRoleId().getRoleName().name();
        List<ComplaintModel> complaints;
        if (userRole.equals("PEMILIK_KOS") || userRole.equals("PENJAGA_KOS") || userRole.equals("ADMIN")) {
            complaints = complaintRepository.findAllComplaintByPropertyId(propertyId);
        } else if (userRole.equals("PENGHUNI")) {
            complaints = complaintRepository.findAllByPropertyIdAndComplainerId(propertyId, currentUser.getId());
        } else {
            throw new CustomExceptions(HttpStatus.FORBIDDEN, "Akses ditolak", null);
        }
        List<ComplaintDTO> complaintDTOs = new ArrayList<>();
        if (complaints.isEmpty()) {
            throw new CustomExceptions(HttpStatus.NOT_FOUND, "Complaint tidak ditemukan", null);
        }
        complaintDTOs = complaints.stream().map(complaint -> ComplaintDTO.mapToDTO(complaint))
                .collect(Collectors.toList());
        return new ApiResponse<>(true, "Complaint ditemukkan", complaintDTOs);
    }

    public ApiResponse<ComplaintDTO> getComplaintById(Long id) {
        ComplaintModel complaint = complaintRepository.findById(id)
                .orElseThrow(
                        () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Complaint dengan ID " + id + " tidak ditemukan",
                                null));
        return new ApiResponse<>(true, "Complaint ditemukan", ComplaintDTO.mapToDTO(complaint));
    }

    public ApiResponse<List<ComplaintDTO>> getAllComplaint() {
        UserModel currentUser = userService.getCurrentUser();
        String userRole = currentUser.getRoleId().getRoleName().name();
        List<ComplaintModel> complaints;
        if (userRole.equals("PEMILIK_KOS")) {
            complaints = complaintRepository.findAllByPropertyOwnerId(currentUser.getId());
        } else if (userRole.equals("PENJAGA_KOS")) {
            complaints = complaintRepository.findAllByPropertyKeeperId(currentUser.getId());
        } else if (userRole.equals("ADMIN")) {
            complaints = complaintRepository.findAll();
        } else if (userRole.equals("PENGHUNI")) {
            complaints = complaintRepository.findAllComplaintByComplainerId(currentUser.getId());
        } else {
            throw new CustomExceptions(HttpStatus.FORBIDDEN, "Akses ditolak", null);
        }
        List<ComplaintDTO> complaintDTOs = new ArrayList<>();
        for (ComplaintModel complaint : complaints) {
            complaintDTOs.add(ComplaintDTO.mapToDTO(complaint));
        }
        return new ApiResponse<>(true, "Complaint ditemukkan", complaintDTOs);
    }

    public ApiResponse<ComplaintDTO> createComplaint(ComplaintDTO request) {
        UserModel complainer = userService.getCurrentUser();
        UnitModel unitModel = unitRepository.findByOccupant(complainer);
        if (unitModel == null) {
            throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Anda belum menghuni kos", null);
        }
        ComplaintModel complaint = new ComplaintModel();
        BeanUtils.copyProperties(request, complaint);
        complaint.setComplainer(complainer);
        complaint.setUserCreate(complainer.getEmail());
        complaint.setProperty(unitModel.getProperty());
        complaint.setUnit(unitModel);
        complaint.setStatus(ComplaintStatus.MENUNGGU_TANGGAPAN);
        // Insert complaint photos if provided
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            List<ComplaintPhotoModel> photoModels = request.getPhotos().stream()
                .map(dto -> {
                    ComplaintPhotoModel photo = new ComplaintPhotoModel();
                    BeanUtils.copyProperties(dto, photo);
                    photo.setComplaint(complaint);
                    photo.setUserCreate(complainer.getEmail());
                    photo.setUploader(complainer);
                    return photo;
                })
                .collect(Collectors.toList());
            complaint.setPhotos(photoModels);
        }
        complaintRepository.save(complaint);
        request.setId(complaint.getId());
        return new ApiResponse<>(true, "Complaint dibuat", ComplaintDTO.mapToDTO(complaint));
    }

    public ApiResponse<ComplaintDTO> updateComplaint(Long id, ComplaintDTO request) {
        UserModel complainer = userService.getCurrentUser();
        ComplaintModel complaint = complaintRepository.findById(id)
                .orElseThrow(
                        () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Complaint " + id + " tidak ditemukan",
                                null));
        if (complaint.getStatus() != ComplaintStatus.MENUNGGU_TANGGAPAN) {
            throw new CustomExceptions(HttpStatus.BAD_REQUEST, "Complaint hanya bisa diupdate jika status masih MENUNGGU_TANGGAPAN", null);
        }
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        // Update complaint photos if provided
        if (request.getPhotos() != null) {
            if (complaint.getPhotos() != null) {
                complaint.getPhotos().clear();
            }
            List<ComplaintPhotoModel> photoModels = request.getPhotos().stream()
                .map(dto -> {
                    ComplaintPhotoModel photo = new ComplaintPhotoModel();
                    BeanUtils.copyProperties(dto, photo);
                    photo.setComplaint(complaint);
                    photo.setUserCreate(complainer.getEmail());
                    photo.setUploader(complainer);
                    return photo;
                })
                .collect(Collectors.toList());
            complaint.setPhotos(photoModels);
        }
        complaintRepository.save(complaint);
        return new ApiResponse<>(true, "Complaint berhasil diperbarui!", ComplaintDTO.mapToDTO(complaint));
    }

    public ApiResponse<Void> deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new CustomExceptions(HttpStatus.NOT_FOUND, "Complaint ID " + id + " tidak ditemukan", null);
        }
        complaintRepository.deleteById(id);
        return new ApiResponse<>(true, "Complaint dihapus", null);
    }

    public ApiResponse<ComplaintDTO> markAsOnProgress(Long id) {
        ComplaintModel complaint = complaintRepository.findById(id)
                .orElseThrow(
                        () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Complaint " + id + " tidak ditemukan",
                                null));
        complaint.setStatus(ComplaintStatus.MASIH_DIKERJAKAN);
        complaintRepository.save(complaint);
        return new ApiResponse<>(true, "Complaint berhasil diperbarui!", ComplaintDTO.mapToDTO(complaint));
    }

    public ApiResponse<ComplaintDTO> markAsDone(Long id) {
        ComplaintModel complaint = complaintRepository.findById(id)
                .orElseThrow(
                        () -> new CustomExceptions(HttpStatus.NOT_FOUND, "Complaint " + id + " tidak ditemukan",
                                null));
        complaint.setStatus(ComplaintStatus.SELESAI);
        complaintRepository.save(complaint);
        return new ApiResponse<>(true, "Complaint berhasil diperbarui!", ComplaintDTO.mapToDTO(complaint));
    }

}
