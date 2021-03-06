package org.fetcher.ui;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.FileDropTarget;

import org.fetcher.Fetcher;
import org.fetcher.Main;
import org.fetcher.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

@Title("fetcher")
@Push
public class FetcherUI extends UI implements Broadcaster.BroadcastListener {
  private static final long serialVersionUID = 1L;
  static Logger logger = LoggerFactory.getLogger(FetcherUI.class);

  private Label activityTicker = new Label("Status");
  Label queryStatus = new Label("Status");
  Label moveStatus = new Label("Status");
  Label queryDBStatus = new Label("Status");
  Label moveDBStatus = new Label("Status");
  Fetcher fetcher = null;

  @Override
  public void detach() {
    Broadcaster.unregister(this);
    super.detach();
  }

  @Override
  protected void init(VaadinRequest request) {
    fetcher = Main.fetcher;
    // Create the content root layout for the UI
    VerticalLayout content = new VerticalLayout();
    content.setSizeFull(); // Use entire window

    setContent(content);

    // ALlow drag and drop anywhere
    @SuppressWarnings("unused")
    FileDropTarget<VerticalLayout> target = new FileDropTarget<>(content, new UploadCSV(this));
    // Display the greeting
    content.addComponent(new Label("Fetcher"));

    GridLayout layout = new GridLayout(2, 2);
    layout.setSpacing(true);
    layout.addComponent(new Label("Called AE:"));
    layout.addComponent(new Label(Main.fetcher.getCalledAET() + "@" + Main.fetcher.getHostname() + ":"
        + Integer.toString(Main.fetcher.getCalledPort())));
    layout.addComponent(new Label("Calling AE Title"));
    layout.addComponent(new Label(Main.fetcher.getCallingAET()));
    layout.addComponent(new Label("Destination AE Title"));
    layout.addComponent(new Label(Main.fetcher.getDestinationAET()));
    content.addComponent(layout);
    content.addComponents(queryStatus, moveStatus, queryDBStatus, moveDBStatus);

    layout = new GridLayout(4, 4);
    layout.setSpacing(true);
    Button startQuery = new Button("Start");
    startQuery.addClickListener(click -> {
      if (!fetcher.isQueryRunning()) {
        startQuery.setCaption("Stop");
        fetcher.startFind();
        Notification.show("starting find");
      } else {
        fetcher.stopFind();
        startQuery.setCaption("Start");
        Notification.show("stopping find");
      }
    });
    if (Main.fetcher.isQueryRunning()) {
      startQuery.setCaption("Stop");
    }
    layout.addComponent(new Label("Query"));
    layout.addComponent(startQuery);
    layout.addComponent(new Label(Main.fetcher.getConcurrentQueries() + " concurrent queries / "
        + Main.fetcher.queriesPerSecond + " queries per second limit"));
    layout.newLine();
    Button startMove = new Button("Start");
    startMove.addClickListener(event -> {
      if (!fetcher.isMoveRunning()) {
        startMove.setCaption("Stop");
        fetcher.startMove();
        Notification.show("starting move");
      } else {
        fetcher.stopMove();
        startMove.setCaption("Start");
        Notification.show("stopping move");
      }
    });
    if (Main.fetcher.isMoveRunning()) {
      startMove.setCaption("Stop");
    }

    layout.addComponent(new Label("Move:"));
    layout.addComponent(startMove);
    layout.addComponent(new Label(Main.fetcher.getConcurrentMoves() + " concurrent moves / "
        + Main.fetcher.imagesPerSecond + " images per second limit"));
    content.addComponent(layout);

    content.addComponentsAndExpand(/* new DemoContentLayout(), */ new QueryGrid(this));
    Broadcaster.register(this);
    content.addComponent(activityTicker);
    update();
  }

  @Override
  public void receiveBroadcast(String message) {
    access(() -> {
      activityTicker.setValue(message);
      update();
    });

  }

  void update() {
    DecimalFormat df = new DecimalFormat("#.00");
    String t = "Move: " + fetcher.movePool.getActiveCount() + " active threads / " + fetcher.moveQueue.size()
        + " pending jobs / " + fetcher.reservoir.getSnapshot().size() + " images per minute";
    t += " / " + df.format(fetcher.imageMeter.getOneMinuteRate()) + " one minute rate / "
        + df.format(fetcher.imageMeter.getFiveMinuteRate()) + " five minute rate";
    moveStatus.setValue(t);
    queryStatus.setValue("Query: " + fetcher.queryPool.getActiveCount() + " active threads / "
        + fetcher.queryQueue.size() + " pending jobs");

    queryDBStatus.setValue("Query: " + Main.queryDAO.queryCount(State.SUCCEEDED) + " succeeded / "
        + Main.queryDAO.queryCount(State.QUEUED) + " queued / " + Main.queryDAO.queryCount(State.FAILED) + " failed / "
        + Main.queryDAO.queryCount(State.CREATED) + " created");
    moveDBStatus.setValue("Move: " + Main.queryDAO.moveCount(State.SUCCEEDED) + " succeeded / "
        + Main.queryDAO.moveCount(State.QUEUED) + " queued / " + Main.queryDAO.moveCount(State.FAILED) + " failed / "
        + Main.queryDAO.moveCount(State.CREATED) + " created");

  }
}
