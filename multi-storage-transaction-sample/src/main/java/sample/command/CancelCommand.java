package reservation.command;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import reservation.Reservation;

@Command(name = "Cancel", description = "Cancel a seat")
public class CancelCommand implements Callable<Integer> {

  @Parameters(index = "0", paramLabel = "CUSTOMER_ID", description = "customer ID")
  private int customerId;

  @Parameters(
      index = "1",
      paramLabel = "CANCELLATIONS",
      description = "cancellations. The format is \"<Seat ID>,<Seat ID>,...\"")
  private String seats;

  @Override
  public Integer call() throws Exception {
    String[] split = seats.split(",", -1);
    int[] seatIds = new int[split.length];

    for (int i = 0; i < split.length; i++) {
      seatIds[i] = Integer.parseInt(split[i]);
    }

    try (Reservation reservation = new Reservation()) {
      System.out.println(reservation.cancel(customerId, seatIds));
    }

    return 0;
  }
}
