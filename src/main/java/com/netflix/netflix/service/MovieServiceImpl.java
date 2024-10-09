package com.netflix.netflix.service;

import com.netflix.netflix.dto.MovieDto;
import com.netflix.netflix.dto.MoviePageResponse;
import com.netflix.netflix.entities.Movie;
import com.netflix.netflix.exceptions.FileExistsException;
import com.netflix.netflix.exceptions.MovieNotFoundException;
import com.netflix.netflix.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService{

    private final MovieRepository movieRepository;

    private final FileService fileService;

    @Value("${project.poster}")
    private String path;

    @Value("${base.url}")
    private String baseUrl;


    public MovieServiceImpl(MovieRepository movieRepository, FileService fileService) {
        this.movieRepository = movieRepository;
        this.fileService = fileService;
    }


    @Override
    public MovieDto addMovie(MovieDto movieDto, MultipartFile file) throws IOException {

        if(Files.exists(Paths.get(path + File.separator + file.getOriginalFilename()))){
            throw new FileExistsException("File already exists! Please enter another file name!");
        }

        String uploadedFileName =  fileService.uploadFile(path, file);
        movieDto.setPoster(uploadedFileName);
        Movie movie = new Movie(
                null,
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()

        );
        Movie saveMovie =  movieRepository.save(movie);
        String posterUrl = baseUrl +"/file/" + uploadedFileName;
        MovieDto response = new MovieDto(
                saveMovie.getMovieID(),
                saveMovie.getTitle(),
                saveMovie.getDirector(),
                saveMovie.getStudio(),
                saveMovie.getMovieCast(),
                saveMovie.getReleaseYear(),
                saveMovie.getPoster(),
                posterUrl
                );

        return response;
    }

    @Override
    public MovieDto getMovie(Integer movieId) {

        Movie movie =  movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found with id = " + movieId));
        String posterUrl = baseUrl +"/file/" + movie.getPoster();
        MovieDto response = new MovieDto(
                movie.getMovieID(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );

        return response;
    }

    @Override
    public List<MovieDto> getAllMovie() {

        List<Movie> movies =  movieRepository.findAll();
        List<MovieDto> moviesDtos = new ArrayList<>();

        for(Movie movie : movies){
            String posterUrl = baseUrl +"/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieID(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            moviesDtos.add(movieDto);
        }



        return moviesDtos;
    }

    @Override
    public MovieDto updateMovie(Integer movieId, MovieDto movieDto, MultipartFile file) throws IOException {
        // 1. check if movie object exists with given movieId
        Movie mv =  movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found with id = " + movieId));


        // 2.if file is null , do nothing
        // if file is not null, then delete existing file associated with the record,
        // and upload the new file
        String fileName = mv.getPoster();
        if (file != null) {
            Files.deleteIfExists(Paths.get(path + File.separator + fileName));
            fileName = fileService.uploadFile(path, file);
        }

        // 3.set movieDto's poster value, according to step 2
        movieDto.setPoster(fileName);


        //4. map it to Movie object
        Movie movie = new Movie(
                mv.getMovieID(),
                movieDto.getTitle(),
                movieDto.getDirector(),
                movieDto.getStudio(),
                movieDto.getMovieCast(),
                movieDto.getReleaseYear(),
                movieDto.getPoster()
        );


        // 5. save the movie object -> return saved movie object
        Movie updateMovie =  movieRepository.save(movie);

        // 6. generate posterUrl for it
        String posterUrl = baseUrl + "/file/" + fileName;

        // 7. map to Movie Dto and return it
        MovieDto response = new MovieDto(
                movie.getMovieID(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getStudio(),
                movie.getMovieCast(),
                movie.getReleaseYear(),
                movie.getPoster(),
                posterUrl
        );

        return response;
    }

    @Override
    public String deleteMovie(Integer movieId) throws MovieNotFoundException, IOException {
        // 1. check if movie object exist in DB
        Movie mv =  movieRepository.findById(movieId).orElseThrow(() -> new MovieNotFoundException("Movie not found with id = " + movieId));
        Integer id = mv.getMovieID();

        //2. delete the file associated with this object
        Files.deleteIfExists(Paths.get(path + File.separator + mv.getPoster()));

        // 3. delete the movie object
        movieRepository.delete(mv);
        return "Movie deleted with id = " + id;
    }

    @Override
    public MoviePageResponse getAllMoviesWithPagination(Integer pageNumber, Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);


        Page<Movie> moviePages = movieRepository.findAll(pageable);
        List<Movie> movies = moviePages.getContent();

        List<MovieDto> moviesDtos = new ArrayList<>();

        for(Movie movie : movies){
            String posterUrl = baseUrl +"/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieID(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            moviesDtos.add(movieDto);
        }

        return new MoviePageResponse(moviesDtos, pageNumber, pageSize,
                moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast());
    }

    @Override
    public MoviePageResponse getAllMoviesWithPaginationAndSorting(Integer pageNumber, Integer pageSize, String sortBy, String dir) {

        Sort sort = dir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);


        Page<Movie> moviePages = movieRepository.findAll(pageable);
        List<Movie> movies = moviePages.getContent();

        List<MovieDto> moviesDtos = new ArrayList<>();

        for(Movie movie : movies){
            String posterUrl = baseUrl +"/file/" + movie.getPoster();
            MovieDto movieDto = new MovieDto(
                    movie.getMovieID(),
                    movie.getTitle(),
                    movie.getDirector(),
                    movie.getStudio(),
                    movie.getMovieCast(),
                    movie.getReleaseYear(),
                    movie.getPoster(),
                    posterUrl
            );
            moviesDtos.add(movieDto);
        }

        return new MoviePageResponse(moviesDtos, pageNumber, pageSize,
                moviePages.getTotalElements(),
                moviePages.getTotalPages(),
                moviePages.isLast());

    }
}
