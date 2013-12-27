/**
 * 
 */
package shuffle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import tags.Song;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * @author Abhinav Sharma
 */
public class TagExtractor {

  private String dirName;

  private ArrayList<HashSet<String>> artists;
  private List<Song> songs = new ArrayList<Song>();
  private List<String> genres = new ArrayList<String>();

  private String ARTISTS_META_FILE_NAME = "ARTISTS_META_FILE.txt";
  private String SONGS_META_FILE_NAME = "SONGS_META_FILE.txt";
  private String GENRES_META_FILE_NAME = "GENRES_META_FILE.txt";

  public TagExtractor(String dirName) {
    this.dirName = dirName;
  }

  public void run() throws IOException, ClassNotFoundException {

    // read genres from file
    readAllGenres();

    // initialize artist according to the number of genres
    artists = new ArrayList<HashSet<String>>(genres.size() + 1);
    for(int i = 0; i < genres.size() + 1; ++i) { artists.add(new HashSet<String>()); }

    // case: we have meta files!
    if (new File(SONGS_META_FILE_NAME).exists() && new File(ARTISTS_META_FILE_NAME).exists()) {
      readMetaFiles();
      return;
    }

    // go through all mp3s in the dir and fill the DSs
    Path fn = FileSystems.getDefault().getPath(dirName);
    Files.walkFileTree(fn, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(".mp3")) {
          try {
            extractId3v2Tags(file);
          } catch (UnsupportedTagException e) {
            e.printStackTrace();
          } catch (InvalidDataException e) {
            e.printStackTrace();
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });

    // make meta files for faster retrieval next time
    writeMetaFiles();
  }

  public List<Song> getSongs() {
    return songs;
  }

  public List<String> getGenres() {
    return genres;
  }

  public ArrayList<HashSet<String>> getArtists() {
    return artists;
  }

  public int getArtistIndex(Song song) {
    int pos = getGenreIndex(song) + 1;
    int index = new ArrayList<String>(artists.get(pos)).indexOf(song.getTag().getArtist());
    for(int i = 0; i <= pos; ++i) {
      index += artists.get(i).size();
    }
    return index;
  }

  public int getGenreIndex(Song song) {
    return genres.indexOf(song.getTag().getGenreDescription());
  }

  private void extractId3v2Tags(Path filename) throws UnsupportedTagException, InvalidDataException, IOException {
    Mp3File songFile = new Mp3File(filename.toString());
    if (songFile.hasId3v2Tag()) {
      ID3v2 id3v2tag = songFile.getId3v2Tag();
      if (id3v2tag.getGenreDescription() != null && id3v2tag.getArtist() != null) {
        Song song = new Song(id3v2tag, filename.toAbsolutePath().toString());
        addArtist(song);
        songs.add(song);
      }
    }
  }

  private void addArtist(Song song) {
    int pos = getGenreIndex(song) + 1;
    artists.get(pos).add(song.getTag().getArtist());
  }

  private void readAllGenres() throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(GENRES_META_FILE_NAME));
    String line;
    while ((line = br.readLine()) != null) {
      genres.add(line);
    }
    br.close();
  }

  private void writeMetaFiles() throws IOException {
    // write artists meta file
    FileOutputStream fos = new FileOutputStream(ARTISTS_META_FILE_NAME);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(artists);
    oos.close();

    // write songs meta file
    writeSongsMetaFiles();
  }

  @SuppressWarnings("unchecked")
  private void readMetaFiles() throws IOException, ClassNotFoundException {

    // read artists meta file
    FileInputStream fos = new FileInputStream(ARTISTS_META_FILE_NAME);
    ObjectInputStream oos = new ObjectInputStream(fos);
    artists = (ArrayList<HashSet<String>>) oos.readObject();
    oos.close();

    // read songs meta file
    readSongsMetaFile();
  }

  private void readSongsMetaFile() throws IOException {
    FileReader fileReader = new FileReader(new File(SONGS_META_FILE_NAME));
    BufferedReader br = new BufferedReader(fileReader);
    String line = null;

    while ((line = br.readLine()) != null) {
      String array[] = line.split("\\|");
      ID3v23Tag tag = new ID3v23Tag();
      tag.setAlbum(array[0]);
      tag.setAlbumArtist(array[1]);
      tag.setArtist(array[2]);
      tag.setTitle(array[3]);
      tag.setGenre(Integer.parseInt(array[4]));
      songs.add(new Song(tag, array[5]));
    }
    br.close();
  }

  private void writeSongsMetaFiles() throws FileNotFoundException, UnsupportedEncodingException {

    PrintWriter writer = new PrintWriter(SONGS_META_FILE_NAME);
    for (Song tag : songs) {
      writer.println(tag.getTag().getAlbum() + "|" + tag.getTag().getAlbumArtist() + "|" + tag.getTag().getArtist() + "|"
          + tag.getTag().getTitle() + "|" + tag.getTag().getGenre() + "|" + tag.getFileName());
    }
    writer.close();
  }

}
