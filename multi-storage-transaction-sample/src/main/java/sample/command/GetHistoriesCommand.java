package reservation.command;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import reservation.Reservation;

@Command(name = "GetHistories", description = "Get history information by customer ID")
public class GetHistoriesCommand implements Callable<Integer> {

  @Parameters(index = "0", paramLabel = "CUSTOMER_ID", description = "customer ID")
  private int customerId;

  @Override
  public Integer call() throws Exception {
    try (Reservation reservation = new Reservation()) {
      System.out.println(reservation.getHistoriesByCustomerId(customerId));
    }
    return 0;
  }
}
