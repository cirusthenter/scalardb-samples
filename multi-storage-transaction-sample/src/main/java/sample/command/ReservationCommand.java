package reservation.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "bin/reservation",
    description = "Reservation application for Microservice Transaction",
    subcommands = {
      LoadInitialDataCommand.class,
      ReserveCommand.class,
      CancelCommand.class,
      GetHistoriesCommand.class,
      GetCustomerInfoCommand.class,
    })

public class ReservationCommand implements Runnable {

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Displays this help message and quits.",
      defaultValue = "true")
  private Boolean showHelp;

  @Override
  public void run() {
    if (showHelp) {
      CommandLine.usage(this, System.out);
    }
  }

  public static void main(String[] args) {
    new CommandLine(new ReservationCommand()).execute(args);
  }
}
