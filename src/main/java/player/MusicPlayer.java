package player;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import tags.Song;

/**
 * @author Abhinav Sharma
 */
public class MusicPlayer {

  private static void initAndShowGUI() {

    JFrame frame = new JFrame("Music Player");
    final JFXPanel fxPanel = new JFXPanel();
    frame.add(fxPanel);
    frame.setBounds(200, 100, 800, 250);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setVisible(true);

    Platform.runLater(new Runnable() {
      public void run() {
        try {
          initFX(fxPanel);
        } catch (URIException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private static void initFX(JFXPanel fxPanel) throws URIException {
    Scene scene = new SceneGenerator().createScene();
    fxPanel.setScene(scene);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        initAndShowGUI();
      }
    });
  }
}

/**
 * @author Abhinav Sharma
 */
class SceneGenerator {

  private ChangeListener<Duration> progressChangeListener;

  private final TextField          tf               = new TextField("D:/My Music");
  private final Button             skip             = new Button("Skip");
  private final Button             play             = new Button("Pause");
  private final Button             go               = new Button("Go");
  private final ProgressBar        progress         = new ProgressBar();

  private final SongFactory        shuffler         = new SongFactory();
  private final MediaView          mediaView        = new MediaView();
  private final Label              currentlyPlaying = new Label();

  public Scene createScene() {

    final StackPane layout = new StackPane();

    skip.setVisible(false);
    play.setVisible(false);
    progress.setVisible(false);

    // when we hit go!
    go.setOnAction(new EventHandler<ActionEvent>() {

      public void handle(ActionEvent actionEvent) {

        // get the first song
        Song song = shuffler.initialize(tf.getText());

        String filename = getURLFileName(song.getFileName());

        // add the song to the media player
        MediaPlayer player = createPlayer(filename);
        mediaView.setMediaPlayer(player);

        // display the name of the currently playing track
        mediaView.mediaPlayerProperty().addListener(new ChangeListener<MediaPlayer>() {
          public void changed(ObservableValue<? extends MediaPlayer> observableValue, MediaPlayer oldPlayer, MediaPlayer newPlayer) {
            setCurrentlyPlaying(newPlayer);
          }
        });

        // enable media buttons
        skip.setVisible(true);
        play.setVisible(true);
        progress.setVisible(true);

        // disable text-box and go
        tf.setVisible(false);
        go.setVisible(false);

        // end of song handler
        player.setOnEndOfMedia(new Runnable() {
          public void run() {
            skip.fire();
          }
        });

        // start playing the first track
        player.play();
        setCurrentlyPlaying(player);
      }
    });

    // when we hit skip!
    skip.setOnAction(new EventHandler<ActionEvent>() {

      public void handle(ActionEvent actionEvent) {

        MediaPlayer player = mediaView.getMediaPlayer();
        MediaPlayer nextPlayer = createPlayer(getURLFileName(shuffler.next(player).getFileName()));

        mediaView.setMediaPlayer(nextPlayer);
        player.currentTimeProperty().removeListener(progressChangeListener);

        player.stop();
        nextPlayer.play();
        setCurrentlyPlaying(nextPlayer);
        nextPlayer.setOnEndOfMedia(new Runnable() {
          public void run() {
            skip.fire();
          }
        });
      }
    });

    // when we hit pause!
    play.setOnAction(new EventHandler<ActionEvent>() {

      public void handle(ActionEvent actionEvent) {
        if ("Pause".equals(play.getText())) {
          mediaView.getMediaPlayer().pause();
          play.setText("Play");
        } else {
          mediaView.getMediaPlayer().play();
          play.setText("Pause");
        }
      }

    });

    // silly invisible button used as a template to get the actual preferred
    // size of the Pause button
    Button invisiblePause = new Button("Pause");
    invisiblePause.setVisible(false);
    play.prefHeightProperty().bind(invisiblePause.heightProperty());
    play.prefWidthProperty().bind(invisiblePause.widthProperty());

    // layout the scene
    layout.setStyle("-fx-background-color: cornsilk; -fx-font-size: 20; -fx-padding: 20; -fx-alignment: center;");
    layout.getChildren().addAll(
        invisiblePause,
        VBoxBuilder
            .create()
            .spacing(10)
            .alignment(Pos.CENTER)
            .children(currentlyPlaying, mediaView, HBoxBuilder.create().spacing(10).alignment(Pos.CENTER).children(tf, go).build(),
                HBoxBuilder.create().spacing(10).alignment(Pos.CENTER).children(skip, play, progress).build()).build());
    progress.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(progress, Priority.ALWAYS);

    return new Scene(layout, 800, 600);
  }

  private String getURLFileName(String filename) {
    try {
      return URIUtil.encodeQuery("file:///" + filename);
    } catch (URIException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getFileNameFromURL(String filename) {
    try {
      return URIUtil.decode(filename);
    } catch (URIException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void setCurrentlyPlaying(final MediaPlayer newPlayer) {

    progress.setProgress(0);

    progressChangeListener = new ChangeListener<Duration>() {
      public void changed(ObservableValue<? extends Duration> observableValue, Duration oldValue, Duration newValue) {
        progress.setProgress(1.0 * newPlayer.getCurrentTime().toMillis() / newPlayer.getTotalDuration().toMillis());
      }
    };

    newPlayer.currentTimeProperty().addListener(progressChangeListener);
    String source = newPlayer.getMedia().getSource();
    source = source.substring(0, source.length() - ".mp3".length());
    source = getFileNameFromURL(source);
    currentlyPlaying.setText("Now Playing: " + source);

  }

  private MediaPlayer createPlayer(String aMediaSrc) {

    final MediaPlayer player = new MediaPlayer(new Media(aMediaSrc));

    player.setOnError(new Runnable() {
      public void run() {
        System.out.println("Media error occurred: " + player.getError());
      }
    });
    return player;
  }
}
