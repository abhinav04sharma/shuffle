package player;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.cell.ComboBoxListCell;
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

import shuffle.Shuffler;
import tags.Song;

/**
 * Example of playing all mp3 audio files in a given directory using a JavaFX
 * MediaView launched from Swing
 */
public class MusicPlayer {
  private static void initAndShowGUI() {
    // This method is invoked on Swing thread
    JFrame frame = new JFrame("FX");
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
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
  }

  private static void initFX(JFXPanel fxPanel) throws URIException {
    // This method is invoked on JavaFX thread
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

class SceneGenerator {
  private static final Shuffler shuffle = new Shuffler("D:/My Music");
  final Label currentlyPlaying = new Label();
  final ProgressBar progress = new ProgressBar();
  private ChangeListener<Duration> progressChangeListener;

  public SceneGenerator() {
    shuffle.initialize();
  }

  public Scene createScene() throws URIException {

    final StackPane layout = new StackPane();
    final SongFactory shuffler = new SongFactory();
    String filename = URIUtil.encodeQuery("file:///" + shuffler.first().getFileName());
    MediaPlayer player = createPlayer(filename);

    final MediaView mediaView = new MediaView(player);
    final Button skip = new Button("Skip");
    final Button play = new Button("Pause");

    player.setOnEndOfMedia(new Runnable() {
      public void run() {
        MediaPlayer player = mediaView.getMediaPlayer();
        player.currentTimeProperty().removeListener(progressChangeListener);

        MediaPlayer nextPlayer = createPlayer("file:///"
            + shuffler.next(player).getFileName().replace("\\", "/").replaceAll(" ", "%20"));

        mediaView.setMediaPlayer(nextPlayer);
        nextPlayer.play();
      }
    });

    // allow the user to skip a track.
    skip.setOnAction(new EventHandler<ActionEvent>() {
      public void handle(ActionEvent actionEvent) {
        MediaPlayer player = mediaView.getMediaPlayer();

        MediaPlayer nextPlayer = createPlayer("file:///"
            + shuffler.next(player).getFileName().replace("\\", "/").replaceAll(" ", "%20"));

        mediaView.setMediaPlayer(nextPlayer);
        player.currentTimeProperty().removeListener(progressChangeListener);

        player.stop();
        nextPlayer.play();
      }
    });

    // allow the user to play or pause a track.
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

    // display the name of the currently playing track.
    mediaView.mediaPlayerProperty().addListener(new ChangeListener<MediaPlayer>() {
      public void changed(ObservableValue<? extends MediaPlayer> observableValue, MediaPlayer oldPlayer, MediaPlayer newPlayer) {
        setCurrentlyPlaying(newPlayer);
      }
    });

    // start playing the first track.
    mediaView.setMediaPlayer(player);
    mediaView.getMediaPlayer().play();
    setCurrentlyPlaying(mediaView.getMediaPlayer());

    // silly invisible button used as a template to get the actual preferred
    // size of the Pause button.
    Button invisiblePause = new Button("Pause");
    invisiblePause.setVisible(false);
    play.prefHeightProperty().bind(invisiblePause.heightProperty());
    play.prefWidthProperty().bind(invisiblePause.widthProperty());

    // layout the scene.
    layout.setStyle("-fx-background-color: cornsilk; -fx-font-size: 20; -fx-padding: 20; -fx-alignment: center;");
    layout.getChildren().addAll(
        invisiblePause,
        VBoxBuilder
            .create()
            .spacing(10)
            .alignment(Pos.CENTER)
            .children(currentlyPlaying, mediaView,
                HBoxBuilder.create().spacing(10).alignment(Pos.CENTER).children(skip, play, progress).build()).build());
    progress.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(progress, Priority.ALWAYS);
    return new Scene(layout, 800, 600);
  }

  /**
   * sets the currently playing label to the label of the new media player and
   * updates the progress monitor.
   */
  private void setCurrentlyPlaying(final MediaPlayer newPlayer) {
    progress.setProgress(0);
    progressChangeListener = new ChangeListener<Duration>() {
      public void changed(ObservableValue<? extends Duration> observableValue, Duration oldValue, Duration newValue) {
        progress.setProgress(1.0 * newPlayer.getCurrentTime().toMillis() / newPlayer.getTotalDuration().toMillis());
      }
    };
    newPlayer.currentTimeProperty().addListener(progressChangeListener);

    String source = newPlayer.getMedia().getSource();
    source = source.substring(0, source.length() - ".mp4".length());
    source = source.substring(source.lastIndexOf("/") + 1).replaceAll("%20", " ");
    currentlyPlaying.setText("Now Playing: " + source);
  }

  /**
   * @return a MediaPlayer for the given source which will report any errors it
   *         encounters
   */
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
