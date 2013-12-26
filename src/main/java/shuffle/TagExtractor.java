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
import java.util.Set;
import java.util.SortedSet;

import tags.Song;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;


/**
 * @author Abhinav Sharma
 * 
 */
public class TagExtractor {

  private String dirName;

  private Set<String> artists = new HashSet<String>();
  private List<Song> songs = new ArrayList<Song>();
  private Set<String> genres = new HashSet<String>();

  private String ARTISTS_META_FILE_NAME = "ARTISTS_META_FILE.txt";
  private String SONGS_META_FILE_NAME = "SONGS_META_FILE.txt";
  private String GENRES_META_FILE_NAME = "GENRES_META_FILE.txt";

  public TagExtractor(String dirName) {
    this.dirName = dirName;
  }

  public void run() throws IOException, ClassNotFoundException {
    readAllGenres();
    if (new File(SONGS_META_FILE_NAME).exists()) {
      readMetaFiles();
      return;
    }

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
    writeMetaFiles();
  }

  public List<Song> getSongs() {
    return songs;
  }

  public Set<String> getGenres() {
    return genres;
  }

  public Set<String> getArtists() {
    return artists;
  }

  private void extractId3v2Tags(Path filename) throws UnsupportedTagException, InvalidDataException, IOException {
    Mp3File song = new Mp3File(filename.toString());
    if (song.hasId3v2Tag()) {
      ID3v2 id3v2tag = song.getId3v2Tag();
      if (id3v2tag.getGenreDescription() != null && id3v2tag.getArtist() != null) {
        artists.add(id3v2tag.getArtist());
        songs.add(new Song(id3v2tag));
        //genres.add(id3v2tag.getGenreDescription());
      }
    }
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
    FileOutputStream fos = new FileOutputStream(ARTISTS_META_FILE_NAME);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(artists);
    oos.close();

    writeMetaId3();

    /*fos = new FileOutputStream(GENRES_META_FILE_NAME);
    oos = new ObjectOutputStream(fos);
    oos.writeObject(genres);
    oos.close();*/
  }

  private void readMetaFiles() throws IOException, ClassNotFoundException {
    FileInputStream fos = new FileInputStream(ARTISTS_META_FILE_NAME);
    ObjectInputStream oos = new ObjectInputStream(fos);
    artists = (SortedSet<String>) oos.readObject();
    oos.close();

    readMetaId3();

    /*fos = new FileInputStream(GENRES_META_FILE_NAME);
    oos = new ObjectInputStream(fos);
    genres = (SortedSet<String>) oos.readObject();
    oos.close();*/
  }

  private void readMetaId3() throws IOException {
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
      songs.add(new Song(tag));
    }
    br.close();
  }

  private void writeMetaId3() throws FileNotFoundException, UnsupportedEncodingException {

    PrintWriter writer = new PrintWriter(SONGS_META_FILE_NAME, "UTF-8");
    for (Song tag : songs) {
      writer.println(tag.getTag().getAlbum() + "|" + tag.getTag().getAlbumArtist() + "|" + tag.getTag().getArtist() + "|"
          + tag.getTag().getTitle() + "|" + tag.getTag().getGenre());
    }
    writer.close();
  }

}
