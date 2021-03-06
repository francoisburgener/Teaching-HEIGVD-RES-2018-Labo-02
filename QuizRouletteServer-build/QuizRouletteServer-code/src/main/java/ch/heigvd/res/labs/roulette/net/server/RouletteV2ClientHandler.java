package ch.heigvd.res.labs.roulette.net.server;

import ch.heigvd.res.labs.roulette.data.EmptyStoreException;
import ch.heigvd.res.labs.roulette.data.IStudentsStore;
import ch.heigvd.res.labs.roulette.data.JsonObjectMapper;
import ch.heigvd.res.labs.roulette.data.StudentsList;
import ch.heigvd.res.labs.roulette.net.protocol.InfoCommandResponse;
import ch.heigvd.res.labs.roulette.net.protocol.ByeCommandReponse;
import ch.heigvd.res.labs.roulette.net.protocol.LoadCommandReponse;
import ch.heigvd.res.labs.roulette.net.protocol.RandomCommandResponse;
import ch.heigvd.res.labs.roulette.net.protocol.RouletteV2Protocol;
import static ch.heigvd.res.labs.roulette.net.server.RouletteV1ClientHandler.LOG;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * This class implements the Roulette protocol (version 2).
 *
 * @author Bryan Curchod, François Burgener
 */
public class RouletteV2ClientHandler implements IClientHandler {

    private IStudentsStore store;

    /**
     * Constructor to set the storage for the students
     * @param store instance to use to store the students
     */
    public RouletteV2ClientHandler(IStudentsStore store) {
        this.store = store;
    }

    /**
     * Handles the interaction with a client, by reading commands on the client's input stream
     * and sending responses on its output stream.
     *
     * @param is input stream to read commands sent by the client
     * @param os output stream to send responses back to the client
     * @throws IOException
     */
    @Override
    public void handleClientConnection(InputStream is, OutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));

        int numberOfCommands = 0;
        int numberOfNewStudents = 0;
        String command;
        boolean done = false; // end the connection
        writer.println("Hello. Online HELP is available. Will you find it?");
        writer.flush();

        while (!done && ((command = reader.readLine()) != null)) {
            numberOfCommands++;
            LOG.log(Level.INFO, "COMMAND: {0}", command);
            switch (command.toUpperCase()) {
                case RouletteV2Protocol.CMD_HELP:
                    writer.println("Commands: " + Arrays.toString(RouletteV2Protocol.SUPPORTED_COMMANDS));
                    break;
                case RouletteV2Protocol.CMD_RANDOM:
                    RandomCommandResponse rcResponse = new RandomCommandResponse();
                    try {
                        rcResponse.setFullname(store.pickRandomStudent().getFullname());
                    } catch (EmptyStoreException ex) {
                        rcResponse.setError("There is no student yet, you cannot pick a random one");
                    }
                    writer.println(JsonObjectMapper.toJson(rcResponse));
                    writer.flush();
                    break;
                case RouletteV2Protocol.CMD_LOAD:
                    writer.println(RouletteV2Protocol.RESPONSE_LOAD_START);
                    writer.flush();
                    int numberOfStudents = store.getNumberOfStudents();
                    try {
                        store.importData(reader);
                    } catch (IOException e) {
                        writer.println(JsonObjectMapper.toJson(new LoadCommandReponse("failure", 0)));
                        writer.flush();
                    }
                    int AllStudents = store.getNumberOfStudents();
                    numberOfNewStudents = AllStudents - numberOfStudents;
                    writer.println(JsonObjectMapper.toJson(new LoadCommandReponse("success", numberOfNewStudents)));
                    writer.flush();
                    break;
                case RouletteV2Protocol.CMD_INFO:
                    InfoCommandResponse response = new InfoCommandResponse(RouletteV2Protocol.VERSION, store.getNumberOfStudents());
                    writer.println(JsonObjectMapper.toJson(response));
                    writer.flush();
                    break;
                case RouletteV2Protocol.CMD_LIST:
                    StudentsList students = new StudentsList();
                    students.setStudents(store.listStudents());
                    writer.println(JsonObjectMapper.toJson(students));
                    writer.flush();
                    break;
                case RouletteV2Protocol.CMD_CLEAR:
                    store.clear();
                    writer.println(RouletteV2Protocol.RESPONSE_CLEAR_DONE);
                    writer.flush();
                    break;
                case RouletteV2Protocol.CMD_BYE:
                    ByeCommandReponse bye = new ByeCommandReponse("success", numberOfCommands);
                    writer.println(JsonObjectMapper.toJson(bye));
                    writer.flush();
                    done = true;
                    break;
                default:
                    writer.println("Huh? please use HELP if you don't know what commands are available.");
                    writer.flush();
                    break;
            }
            writer.flush();
        }
    }

}
