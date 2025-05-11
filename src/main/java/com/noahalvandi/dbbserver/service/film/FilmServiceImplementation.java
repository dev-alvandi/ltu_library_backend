package com.noahalvandi.dbbserver.service.film;

import com.noahalvandi.dbbserver.dto.projection.LanguageCount;
import com.noahalvandi.dbbserver.dto.projection.film.FilmFilterCriteria;
import com.noahalvandi.dbbserver.dto.projection.film.FilmsReleasedDateRange;
import com.noahalvandi.dbbserver.dto.request.film.FilmCopyRequest;
import com.noahalvandi.dbbserver.dto.request.film.FilmRequest;
import com.noahalvandi.dbbserver.dto.request.mapper.FilmRequestMapper;
import com.noahalvandi.dbbserver.dto.response.film.FilmCopyResponse;
import com.noahalvandi.dbbserver.dto.response.film.FilmResponse;
import com.noahalvandi.dbbserver.dto.response.film.FilmSuggestionsResponse;
import com.noahalvandi.dbbserver.dto.response.mapper.film.FilmCopyResponseMapper;
import com.noahalvandi.dbbserver.dto.response.mapper.film.FilmResponseMapper;
import com.noahalvandi.dbbserver.exception.ResourceException;
import com.noahalvandi.dbbserver.model.*;
import com.noahalvandi.dbbserver.repository.film.FilmCategoryRepository;
import com.noahalvandi.dbbserver.repository.film.FilmCopyRepository;
import com.noahalvandi.dbbserver.repository.film.FilmRepository;
import com.noahalvandi.dbbserver.service.S3Service;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmServiceImplementation implements FilmService {

    private final FilmRepository filmRepository;
    private final FilmCopyRepository filmCopyRepository;
    private final FilmCategoryRepository filmCategoryRepository;
    private final S3Service s3Service;

    @Override
    public Page<FilmResponse> getAllFilms(Pageable pageable) {
        Page<Film> films = filmRepository.findAllAvailableFilmsToBorrow(pageable);
        Page<FilmResponse> pageFilmDtos = films.map(FilmResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(pageFilmDtos);

        addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(pageFilmDtos);
        return pageFilmDtos;
    }

    
    @Override
    public Map<String, Long> getAllLanguagesAndTheirCounts() {
        List<LanguageCount> counts = filmRepository.getAllLanguagesAndTheirCounts();
        return counts.stream()
                .collect(Collectors.toMap(LanguageCount::getLanguage, LanguageCount::getCount));
    }


    @Override
    public List<String> getAllLanguages() {
        return filmRepository.getAllLanguages();
    }


    @Override
    public FilmsReleasedDateRange getFilmsReleasedDateRange() {
        return filmRepository.getFilmsReleasedDateRange();
    }


    @Override
    public Page<FilmResponse> getFilteredFilms(Pageable pageable, FilmFilterCriteria filmFilterCriteria) {
        Boolean isAvailable = filmFilterCriteria.isAvailable();
        List<String> categories = filmFilterCriteria.getCategories();
        List<String> languages = filmFilterCriteria.getLanguages();

        if (categories != null && categories.isEmpty()) categories = null;
        if (languages != null && languages.isEmpty()) languages = null;

        int ageRating = 0;

        try {
            ageRating = filmFilterCriteria.getAgeRating();
        } catch (NumberFormatException e) {
            System.out.println("Invalid integer: " + ageRating);
            throw ResourceException.forbidden("Age rating should be a number!");
        }

        Page<Film> films = filmRepository.findFilmsByFilters(
                isAvailable,
                filmFilterCriteria.getMinReleasedDate(),
                filmFilterCriteria.getMaxReleasedDate(),
                categories,
                languages,
                ageRating,
                pageable
        );

        Page<FilmResponse> filmResponses = films.map(FilmResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(filmResponses);

        return addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(filmResponses);
    }


    @Override
    public FilmSuggestionsResponse getSuggestions(String query) {
        FilmSuggestionsResponse filmSuggestionsResponse = new FilmSuggestionsResponse();

        filmSuggestionsResponse.setTitle(filmRepository.findDistinctTitlesByQuery(query));
        filmSuggestionsResponse.setDirector(filmRepository.findDistinctDirectorsByQuery(query));
        filmSuggestionsResponse.setCountry(filmRepository.findDistinctCountriesByQuery(query));

        return filmSuggestionsResponse;
    }


    @Override
    public Page<FilmResponse> getSearchedFilms(String query, Pageable pageable, FilmFilterCriteria filters) {
        Page<FilmResponse> filmResponses = filmRepository.searchWithFilters(
                query,
                filters.isAvailable(),
                filters.getMinReleasedDate(),
                filters.getMaxReleasedDate(),
                filters.getCategories(),
                filters.getLanguages(),
                filters.getAgeRating(),
                pageable
        ).map(FilmResponseMapper::toDto);

        s3Service.injectS3ImageUrlIntoDto(filmResponses);

        return addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(filmResponses);
    }


    @Override
    public FilmResponse createFilm(FilmRequest filmRequest, MultipartFile image) throws Exception {
        // 1. Handle FilmCategory
        FilmCategory filmCategory = filmCategoryRepository.findByGenreIgnoreCase(filmRequest.getFilmCategory())
                .orElseGet(() -> {
                    FilmCategory newCategory = new FilmCategory();
                    newCategory.setGenre(filmRequest.getFilmCategory());
                    return filmCategoryRepository.save(newCategory);
                });

        // 2. Upload image to S3 (if present)
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            UUID tempId = UUID.randomUUID(); // temporary ID to construct S3 key
            imageUrl = s3Service.uploadResourceImage("film", tempId, image);
        }

        // 3. Save Film
        Film film = FilmRequestMapper.toEntity(filmRequest);
        film.setFilmCategory(filmCategory);
        film.setImageUrl(imageUrl);

        Film savedFilm = filmRepository.save(film);

        // 4. Create one available FilmCopy
        FilmCopy filmCopy = new FilmCopy();
        filmCopy.setFilm(savedFilm);
        filmCopy.setPhysicalLocation("Inventory");
        filmCopy.setIsReferenceCopy(IsItemReferenceCopy.TRUE);
        filmCopy.setStatus(ItemStatus.AVAILABLE);

        // Generate Barcode
        String barcodeText = "FILM-" +
                film.getFilmId().toString().substring(0, 8) + "-" +
                UUID.randomUUID().toString().substring(0, 8);

        filmCopy.setBarcode(barcodeText);

        FilmCopy savedFilmCopy = filmCopyRepository.save(filmCopy);

        s3Service.uploadResourceBarcodeImage("film", savedFilm.getImageUrl(), savedFilmCopy.getFilmCopyId(), savedFilmCopy.getBarcode());

        // 5. Return mapped DTO
        FilmResponse response = FilmResponseMapper.toDto(film);
        response.setNumberOfCopies(1);
        response.setNumberOfAvailableToBorrowCopies(1);

        return response;
    }


    @Override
    public FilmResponse updateFilm(FilmRequest filmRequest, MultipartFile image) throws IOException {
        Film film = filmRepository.findById(filmRequest.getFilmId())
                .orElseThrow(() -> ResourceException.notFound("Film not found"));

        // Find or create a category
        FilmCategory filmCategory = filmCategoryRepository.findByGenreIgnoreCase(filmRequest.getFilmCategory())
                .orElseGet(() -> {
                    FilmCategory newFilmCategory = new FilmCategory();
                    newFilmCategory.setGenre(filmRequest.getFilmCategory());
                    return filmCategoryRepository.save(newFilmCategory);
                });

        // Handle image upload if a new image is provided
        if (image != null && !image.isEmpty()) {
            // Delete the old image from S3 if it exists
            String existingImageUrl = film.getImageUrl();
            if (existingImageUrl != null && !existingImageUrl.isBlank()) {
                // Extract key from full URL: everything after the bucket domain
                String key = existingImageUrl.substring(existingImageUrl.indexOf("films/"));
                s3Service.deleteFile(key);
            }

            // Upload new image
            String newImageUrl = s3Service.uploadResourceImage("film", film.getFilmId(), image);
            film.setImageUrl(newImageUrl);
        }

        // Update fields
        film.setTitle(filmRequest.getTitle());
        film.setDirector(filmRequest.getDirector());
        film.setCountry(filmRequest.getCountry());
        film.setReleasedDate(filmRequest.getReleasedDate());
        film.setLanguage(filmRequest.getLanguage());
        film.setAgeRating(filmRequest.getAgeRating());
        film.setFilmCategory(filmCategory);

        filmRepository.save(film);

        FilmResponse response = FilmResponseMapper.toDto(film);
        response.setNumberOfCopies(filmCopyRepository.countAllNonReferenceCopiesByFilmId(film.getFilmId()));
        response.setNumberOfAvailableToBorrowCopies(filmCopyRepository.numberOfAvailableFilmCopiesToBorrow(film.getFilmId()));

        return response;
    }


    @Override
    public FilmResponse getRequestedFilm(UUID filmId) {
        Film film = filmRepository.findFilmByFilmId(filmId);

        FilmResponse fetchedFilm = FilmResponseMapper.toDto(film);

        if (fetchedFilm.getImageUrl() != null) {
            String presignedUrl = s3Service.generatePresignedUrl(fetchedFilm.getImageUrl(), GlobalConstants.CLOUD_URL_EXPIRATION_TIME_IN_MINUTES);
            fetchedFilm.setImageUrl(presignedUrl);
        }

        return fetchedFilm;
    }


    @Override
    public Page<FilmCopyResponse> getFilmCopiesByFilmId(UUID filmId, Pageable pageable) {
        // Fetch the Film entity first
        Film film = filmRepository.findFilmByFilmId(filmId);

        if (film == null) {
            throw ResourceException.notFound("Film not found");
        }

        // Fetch FilmCopy entities
        Page<FilmCopy> filmCopies = filmCopyRepository.findFilmCopiesByFilmId(filmId, pageable);

        // Map and enrich each FilmCopy
        Page<FilmCopyResponse> filmCopyResponses =  filmCopies.map(filmCopy -> {
            FilmCopyResponse response = FilmCopyResponseMapper.toDto(filmCopy);
            if (filmCopy.getBarcode() != null) {
                String barcodeKey = String.format(
                        "films/%s/barcodes/%s/%s.png",
                        film.getImageUrl().split("/")[1],
                        filmCopy.getFilmCopyId(),
                        filmCopy.getBarcode()
                );

                String barcodeUrl = s3Service.generatePresignedUrl(barcodeKey, 5);
                response.setBarcodeUrl(barcodeUrl);
            }
            return response;
        });

        return filmCopyResponses;
    }


    @Override
    public FilmCopyResponse createFilmCopy(UUID filmId, FilmCopyRequest request) throws Exception {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> ResourceException.notFound("Film not found"));

        FilmCopy newCopy = new FilmCopy();
        newCopy.setFilm(film);
        newCopy.setPhysicalLocation(request.getPhysicalLocation());
        newCopy.setIsReferenceCopy(
                request.isItemReferenceCopy() ? IsItemReferenceCopy.TRUE : IsItemReferenceCopy.FALSE
        );
        newCopy.setStatus(ItemStatus.AVAILABLE);

        // Generate new barcode
        String barcodeText = "FILM-" + 
                film.getFilmId().toString().substring(0, 8) + 
                "-" + 
                UUID.randomUUID().toString().substring(0, 8);
        
        newCopy.setBarcode(barcodeText);

        FilmCopy savedCopy = filmCopyRepository.save(newCopy);
        
        // Upload barcode to S3
        s3Service.uploadResourceBarcodeImage("film", film.getImageUrl(), savedCopy.getFilmCopyId(), savedCopy.getBarcode());

        return FilmCopyResponseMapper.toDto(savedCopy);
    }


    @Override
    @Transactional
    public void deleteFilmAndCopies(UUID filmId) {
        // Find the Film first
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> ResourceException.notFound("Film not found"));

        // Delete Film Image from S3 if exists
        if (film.getImageUrl() != null && !film.getImageUrl().isBlank()) {
            s3Service.deleteFile(film.getImageUrl());
        }

        // Fetch all FilmCopies linked to the Film
        List<FilmCopy> filmCopies = filmCopyRepository.findAllByFilmFilmId(filmId);

        // Delete each barcode image from S3
        for (FilmCopy copy : filmCopies) {
            if (copy.getBarcode() != null && !copy.getBarcode().isBlank()) {
                String barcodeKey = String.format(
                        "films/%s/barcodes/%s/%s.png",
                        film.getImageUrl().split("/")[1],
                        copy.getFilmCopyId(),
                        copy.getBarcode()
                );
                s3Service.deleteFile(barcodeKey);
            }
        }

        // Delete FilmCopies from DB
        filmCopyRepository.deleteFilmCopiesByFilmId(filmId);
        
        // Delete the Film itself
        filmRepository.deleteByFilmId(filmId);
    }


    @Override
    @Transactional
    public void deleteFilmCopyByFilmCopyId(UUID filmCopyId) {
        // Find the FilmCopy first
        FilmCopy filmCopy = filmCopyRepository.findById(filmCopyId)
                .orElseThrow(() -> ResourceException.notFound("FilmCopy not found"));

        // Build the S3 barcode key
        String barcodeKey = String.format(
                "films/%s/barcodes/%s/%s.png",
                filmCopy.getFilm().getImageUrl().split("/")[1],
                filmCopy.getFilmCopyId(),
                filmCopy.getBarcode()
        );

        // Delete the barcode from S3
        s3Service.deleteFile(barcodeKey);

        // Delete the FilmCopy from the database
        filmCopyRepository.deleteById(filmCopyId);
    }


    @Override
    @Transactional
    public FilmCopyResponse updateFilmCopy(UUID filmCopyId, FilmCopyRequest request) throws Exception {
        // Fetch the FilmCopy
        FilmCopy filmCopy = filmCopyRepository.findById(filmCopyId)
                .orElseThrow(() -> ResourceException.notFound("Film copy not found"));

        if (request.getStatus() != null) {
            filmCopy.setStatus(ItemStatus.valueOf(request.getStatus()));
        }

        // Update fields
        filmCopy.setIsReferenceCopy(
                request.isItemReferenceCopy() ? IsItemReferenceCopy.TRUE : IsItemReferenceCopy.FALSE
        );

        if (request.getPhysicalLocation() != null && !request.getPhysicalLocation().isBlank()) {
            filmCopy.setPhysicalLocation(request.getPhysicalLocation());
        }

        // Save the updated FilmCopy
        FilmCopy updatedCopy = filmCopyRepository.save(filmCopy);

        // Map and return the response
        return FilmCopyResponseMapper.toDto(updatedCopy);
    }

    private Page<FilmResponse> addingAvailableFilmCopiesAndNumberOfNonReferenceCopiesToFilmResponse(Page<FilmResponse> filmResponses) {
        filmResponses.getContent().forEach(filmResponse -> {
            filmResponse.setNumberOfCopies(
                    filmCopyRepository.countAllNonReferenceCopiesByFilmId(filmResponse.getFilmId())
            );
            filmResponse.setNumberOfAvailableToBorrowCopies(
                    filmCopyRepository.numberOfAvailableFilmCopiesToBorrow(filmResponse.getFilmId())
            );
        });

        return filmResponses;
    }
}
