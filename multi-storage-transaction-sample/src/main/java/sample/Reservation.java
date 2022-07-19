package reservation;

import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.db.service.TransactionFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Reservation implements AutoCloseable {

  private final DistributedTransactionManager manager;

  public Reservation() throws IOException {
    // Create a transaction manager object
    TransactionFactory factory =
        new TransactionFactory(new DatabaseConfig(new File("database.properties")));
    manager = factory.getTransactionManager();
  }

  public void loadInitialData() throws TransactionException {
    DistributedTransaction transaction = null;
    try {
      transaction = manager.start();
      loadCustomerIfNotExists(transaction, 1, "Elon Musk");
      loadCustomerIfNotExists(transaction, 2, "Tony Stark");
      loadCustomerIfNotExists(transaction, 3, "Steve Jobs");
      loadSeatIfNotExists(transaction, 1, "S1", -1);
      loadSeatIfNotExists(transaction, 2, "S2", -1);
      loadSeatIfNotExists(transaction, 3, "A1", -1);
      loadSeatIfNotExists(transaction, 4, "A2", -1);
      loadSeatIfNotExists(transaction, 5, "A3", -1);
      loadSeatIfNotExists(transaction, 6, "B1", -1);
      loadSeatIfNotExists(transaction, 7, "B2", -1);
      loadSeatIfNotExists(transaction, 8, "B3", -1);
      loadSeatIfNotExists(transaction, 9, "B4", -1);
      loadSeatIfNotExists(transaction, 10, "C1", -1);
      loadSeatIfNotExists(transaction, 11, "C2", -1);
      loadSeatIfNotExists(transaction, 12, "C3", -1);
      loadSeatIfNotExists(transaction, 13, "C4", -1);
      loadSeatIfNotExists(transaction, 14, "C5", -1);
      transaction.commit();
    } catch (TransactionException e) {
      if (transaction != null) {
        // If an error occurs, abort the transaction
        transaction.abort();
      }
      throw e;
    }
  }

  private void loadCustomerIfNotExists(DistributedTransaction transaction, int customerId,
      String name) throws TransactionException {
    Optional<Result> customer = transaction.get(
        new Get(new Key("customer_id", customerId)).forNamespace("customer").forTable("customers"));
    if (!customer.isPresent()) {
      transaction.put(new Put(new Key("customer_id", customerId)).withValue("name", name)
          .forNamespace("customer").forTable("customers"));
    }
  }

  private void loadSeatIfNotExists(DistributedTransaction transaction, int seatId, String name,
      int reservedBy) throws TransactionException {
    Optional<Result> seat =
        transaction.get(new Get(new Key("seat_id", seatId)).forNamespace("seat").forTable("seats"));
    if (!seat.isPresent()) {
      transaction.put(new Put(new Key("seat_id", seatId)).withValue("name", name)
          .withValue("reserved_by", reservedBy).forNamespace("seat").forTable("seats"));
    }
  }

  public String getCustomerInfo(int customerId) throws TransactionException {
    DistributedTransaction transaction = null;
    try {
      // Start a transaction
      transaction = manager.start();

      // Retrieve the customer info for the specified customer ID from the customers table
      Optional<Result> customer = transaction.get(new Get(new Key("customer_id", customerId))
          .forNamespace("customer").forTable("customers"));

      if (!customer.isPresent()) {
        // If the customer info the specified customer ID doesn't exist, throw an exception
        throw new RuntimeException("Customer not found");
      }

      // Commit the transaction (even when the transaction is read-only, we need to commit)
      transaction.commit();

      // Return the customer info as a JSON format
      return String.format("\"id\": %d, \"name\": \"%s\"", customerId,
          customer.get().getValue("name").get().getAsString().get());
    } catch (Exception e) {
      if (transaction != null) {
        // If an error occurs, abort the transaction
        transaction.abort();
      }
      throw e;
    }
  }

  public String reserve(int customerId, int[] seatIds) throws TransactionException {

    DistributedTransaction transaction = null;
    try {
      // Start a transaction
      transaction = manager.start();

      Optional<Result> customerOptional =
          transaction.get(new Get(new Key("customer_id", customerId)).forNamespace("customer")
              .forTable("customers"));
      if (!customerOptional.isPresent()) {
        throw new RuntimeException("Customer " + String.valueOf(customerId) + " not exists");
      }

      String[] historyIds = new String[seatIds.length];
      for (int i = 0; i < seatIds.length; i++) {
        int seatId = seatIds[i];
        String historyId = UUID.randomUUID().toString();
        historyIds[i] = historyId;

        // Retrieve the seat info from the items table
        Optional<Result> seatOptional = transaction
            .get(new Get(new Key("seat_id", seatId)).forNamespace("seat").forTable("seats"));
        if (!seatOptional.isPresent()) {
          throw new RuntimeException("Seat " + String.valueOf(seatId) + " not found");
        } else if (seatOptional.get().getValue("reserved_by").get().getAsInt() != -1) {
          throw new RuntimeException("Seat " + String.valueOf(seatId) + " already reserved");
        }

        // Put the order info into the orders table
        transaction.put(new Put(new Key("customer_id", customerId),
            new Key("timestamp", System.currentTimeMillis())).withValue("history_id", historyId)
                .withValue("operation", "reserve").withValue("seat_id", seatId)
                .forNamespace("history").forTable("histories"));
        // Update seat
        transaction.put(new Put(new Key("seat_id", seatId)).withValue("reserved_by", customerId)
            .forNamespace("seat").forTable("seats"));
      }

      // Commit the transaction
      transaction.commit();

      // Return the order id
      String message = String.format("{\"history_id\": \"%s\"}", historyIds[0]);
      for (int i = 1; i < historyIds.length; i++) {
        message += String.format(",\n{\"history_id\": \"%s\"}", historyIds[i]);
      }
      return message;
    } catch (Exception e) {
      if (transaction != null) {
        // If an error occurs, abort the transaction
        transaction.abort();
      }
      throw e;
    }
  }

  public String cancel(int customerId, int[] seatIds) throws TransactionException {

    DistributedTransaction transaction = null;
    try {
      // Start a transaction
      transaction = manager.start();

      Optional<Result> customerOptional =
          transaction.get(new Get(new Key("customer_id", customerId)).forNamespace("customer")
              .forTable("customers"));
      if (!customerOptional.isPresent()) {
        throw new RuntimeException("Customer " + String.valueOf(customerId) + " not exists");
      }

      String[] historyIds = new String[seatIds.length];
      for (int i = 0; i < seatIds.length; i++) {
        int seatId = seatIds[i];

        // Retrieve the seat info from the items table
        Optional<Result> seatOptional = transaction
            .get(new Get(new Key("seat_id", seatId)).forNamespace("seat").forTable("seats"));
        if (!seatOptional.isPresent()) {
          throw new RuntimeException("Seat " + String.valueOf(seatId) + " not found");
        }
        Result seat = seatOptional.get();
        if (seat.getValue("reserved_by").get().getAsInt() == -1) {
          throw new RuntimeException("Seat " + String.valueOf(seatId) + " not reserved");
        } else if (seat.getValue("reserved_by").get().getAsInt() != customerId) {
          throw new RuntimeException(
              "Seat " + String.valueOf(seatId) + " not reserved by " + String.valueOf(customerId));
        }

        String historyId = UUID.randomUUID().toString();
        historyIds[i] = historyId;

        // Delete reservation
        transaction.put(new Put(new Key("seat_id", seatId)).withValue("reserved_by", -1)
            .forNamespace("seat").forTable("seats"));

        // Put the order info into the orders table
        transaction.put(new Put(new Key("customer_id", customerId),
            new Key("timestamp", System.currentTimeMillis())).withValue("history_id", historyId)
                .withValue("operation", "cancel").withValue("seat_id", seatId)
                .forNamespace("history").forTable("histories"));
      }

      // Commit the transaction
      transaction.commit();

      // Return the order id
      String message = String.format("{\"history_id\": \"%s\"}", historyIds[0]);
      for (int i = 1; i < historyIds.length; i++) {
        message += String.format(",\n{\"history_id\": \"%s\"}", historyIds[i]);
      }
      return message;
    } catch (Exception e) {
      if (transaction != null) {
        // If an error occurs, abort the transaction
        transaction.abort();
      }
      throw e;
    }
  }


  public String getHistoriesByCustomerId(int customerId) throws TransactionException {
    DistributedTransaction transaction = null;
    try {
      // Start a transaction
      transaction = manager.start();

      // Retrieve the reservation info for the customer ID from the reservations table
      List<Result> histories = transaction.scan(new Scan(new Key("customer_id", customerId))
          .forNamespace("history").forTable("histories"));

      // Make history JSONs for the historys of the customer
      List<String> historyJsons = new ArrayList<>();
      for (Result history : histories) {
        Date date = new Date(history.getValue("timestamp").get().getAsLong());
        DateFormat formatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("JST"));
        String dateFormatted = formatter.format(date);
        int seatId = history.getValue("seat_id").get().getAsInt();
        Optional<Result> seatOptional = transaction
            .get(new Get(new Key("seat_id", seatId)).forNamespace("seat").forTable("seats"));
        String seat_name = seatOptional.get().getValue("name").get().getAsString().get();
        historyJsons.add(String.format("operation: %s, seat: %s, time: %s",
            history.getValue("operation").get().getAsString().get(), seat_name, dateFormatted));
      }

      // Commit the transaction (even when the transaction is read-only, we need to commit)
      transaction.commit();

      // Return the order info as a JSON format
      return String.format("%s", String.join("\n", historyJsons));
    } catch (Exception e) {
      if (transaction != null) {
        // If an error occurs, abort the transaction
        transaction.abort();
      }
      throw e;
    }
  }

  @Override
  public void close() {
    manager.close();
  }
}
