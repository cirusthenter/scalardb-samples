package reservation.command;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import reservation.Reservation;

@Command(name = "LoadInitialData", description = "Load initial data")
public class LoadInitialDataCommand implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    try (Reservation reservation = new Reservation()) {
      reservation.loadInitialData();
    }
    return 0;
  }
}
