/**
 * 
 */
package tags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * @author Abhinav Sharma
 */
public class TagExtractor {

  private final String               ARTISTS_META_FILE_PREFIX = "ARTISTS_META_FILE";
  private final String               SONGS_META_FILE_PREFIX   = "SONGS_META_FILE";

  private final String               GENRES_META_FILE         = "GENRES_META_FILE.txt";

  private String                     musicDirectory;
  private final String               dataDirectory;

  private String                     genreFileName;
  private String                     artistFileName;
  private String                     songFileName;

  private ArrayList<HashSet<String>> artists;
  private final List<Song>           songs                    = new ArrayList<Song>();
  private final List<String>         genres                   = new ArrayList<String>();

  private final boolean              fileListProvided;
  private List<String>               fileList;

  public TagExtractor(String musicDirectory, String dataDirectory) throws NoSuchAlgorithmException {
    this.fileListProvided = false;
    this.musicDirectory = musicDirectory;
    this.dataDirectory = dataDirectory;
    initializeFileNames();
  }

  public TagExtractor(List<String> fileNames, String dataDirectory) throws NoSuchAlgorithmException {
    this.fileListProvided = true;
    this.fileList = fileNames;
    this.dataDirectory = dataDirectory;
    initializeFileNames();
  }

  private void initializeFileNames() throws NoSuchAlgorithmException {
    this.artistFileName = new File(this.dataDirectory, getFileName(musicDirectory, ARTISTS_META_FILE_PREFIX)).getAbsolutePath();
    this.songFileName = new File(this.dataDirectory, getFileName(musicDirectory, SONGS_META_FILE_PREFIX)).getAbsolutePath();
    this.genreFileName = new File(this.dataDirectory, GENRES_META_FILE).getAbsolutePath();
  }

  public void run() throws IOException, ClassNotFoundException {

    // read genres from file
    try {
      readAllGenres();
    } catch (URISyntaxException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    // initialize artist according to the number of genres
    artists = new ArrayList<HashSet<String>>(genres.size() + 1);
    for (int i = 0; i < genres.size() + 1; ++i) {
      artists.add(new HashSet<String>());
    }

    // case: we have meta files!
    if (new File(songFileName).exists() && new File(artistFileName).exists()) {
      readMetaFiles();
      return;
    }

    // we got a list of file names, just read them (no need to recurse)
    if (fileListProvided) {
      for (String fileName : fileList) {
        try {
          extractId3v2Tags(fileName);
        } catch (UnsupportedTagException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (InvalidDataException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    } else {
      // walk all mp3 and fetch tags
      walkDir(musicDirectory);
    }

    // make meta files for faster retrieval next time
    writeMetaFiles();
  }

  public List<Song> getSongs() {
    return songs;
  }

  public List<String> getGenres() {
    return genres;
  }

  public List<String> getArtists() {
    List<String> ret = new ArrayList<String>();
    for (HashSet<String> hs : artists) {
      ret.addAll(hs);
    }
    return ret;
  }

  public int getArtistIndex(Song song) {
    int pos = getGenreIndex(song) + 1;
    int index = new ArrayList<String>(artists.get(pos)).indexOf(song.getTag().getArtist());
    for (int i = 0; i <= pos; ++i) {
      index += artists.get(i).size();
    }
    return index;
  }

  public int getGenreIndex(Song song) {
    return genres.indexOf(song.getTag().getGenreDescription());
  }

  private void walkDir(String dir) {
    File[] files = new File(dir).listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        walkDir(file.toString());
      } else if (file.toString().endsWith(".mp3")) {
        try {
          extractId3v2Tags(file.getAbsolutePath());
        } catch (UnsupportedTagException e) {
          e.printStackTrace();
        } catch (InvalidDataException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private String getFileName(String dir, String prefix) throws NoSuchAlgorithmException {
    return DigestUtils.md5Hex(prefix + dir) + ".txt";
  }

  private void extractId3v2Tags(String filename) throws UnsupportedTagException, InvalidDataException, IOException {
    Mp3File songFile = new Mp3File(filename);
    if (songFile.hasId3v2Tag()) {
      ID3v2 id3v2tag = songFile.getId3v2Tag();
      if (id3v2tag.getGenreDescription() != null && id3v2tag.getArtist() != null) {
        Song song = new Song(id3v2tag, filename);
        // TODO: we're adding it in the end, that means they are not logically
        // related. Not cool!
        if (!genres.contains(song.getTag().getGenreDescription())) {
          genres.add(song.getTag().getGenreDescription());
          artists.add(new HashSet<String>());
        }
        addArtist(song);
        songs.add(song);
      }
    }
  }

  private void addArtist(Song song) {
    int pos = getGenreIndex(song) + 1;
    artists.get(pos).add(song.getTag().getArtist());
  }

  private void readAllGenres() throws IOException, URISyntaxException {
    BufferedReader br;
    File fsGenreFile = new File(genreFileName);
    // case: get from file-system
    if (fsGenreFile.exists()) {
      br = new BufferedReader(new FileReader(fsGenreFile));
      // case: not present in file-system, get from within jar
    } else {
      br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + GENRES_META_FILE)));
    }
    String line;
    while ((line = br.readLine()) != null) {
      genres.add(line);
    }
    br.close();
  }

  private void writeMetaFiles() throws IOException {
    // make data dir if it does not exists
    File dataDir = new File(dataDirectory);
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
    // write artists meta file
    FileOutputStream fos = new FileOutputStream(artistFileName);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(artists);
    oos.close();

    // write songs meta file
    writeSongsMetaFile();

    // write genre meta file
    try {
      writeGenreMetaFile();
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  private void readMetaFiles() throws IOException, ClassNotFoundException {

    // read artists meta file
    FileInputStream fos = new FileInputStream(artistFileName);
    ObjectInputStream oos = new ObjectInputStream(fos);
    artists = (ArrayList<HashSet<String>>) oos.readObject();
    oos.close();

    // read songs meta file
    readSongsMetaFile();
  }

  private void readSongsMetaFile() throws IOException {
    FileReader fileReader = new FileReader(new File(songFileName));
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

  private void writeSongsMetaFile() throws FileNotFoundException, UnsupportedEncodingException {

    PrintWriter writer = new PrintWriter(songFileName);
    for (Song tag : songs) {
      writer.println(tag.getTag().getAlbum() + "|" + tag.getTag().getAlbumArtist() + "|" + tag.getTag().getArtist() + "|"
          + tag.getTag().getTitle() + "|" + tag.getTag().getGenre() + "|" + tag.getFileName());
    }
    writer.close();
  }

  private void writeGenreMetaFile() throws FileNotFoundException, URISyntaxException {
    PrintWriter writer = new PrintWriter(new File(genreFileName));
    for (String genre : genres) {
      writer.println(genre);
    }
    writer.close();
  }
}
