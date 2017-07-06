package jp.co.ntt.oss;

import java.sql.Connection;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import jp.co.ntt.oss.data.DatabaseResource;
import jp.co.ntt.oss.data.SyncDatabaseDAO;
import jp.co.ntt.oss.utility.PropertyCtrl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

public class DropCommand implements SyncDatabaseCommand {
	private static Logger log = Logger.getLogger(DropCommand.class);
	private static PropertyCtrl mProperty = PropertyCtrl.getInstance();

	private boolean showHelp = false;
	private String schema = null;
	private String table = null;
	private String master = null;

	public DropCommand(final CommandLine commandLine)
			throws SyncDatabaseException {
		if (commandLine.hasOption("help")) {
			showHelp = true;
			return;
		}

		/* checking necessary argument */
		if (!commandLine.hasOption("master")
				|| !commandLine.hasOption("schema")
				|| !commandLine.hasOption("table")) {
			showHelp();
			throw new SyncDatabaseException("error.command");
		}

		/* set argument */
		schema = commandLine.getOptionValue("schema");
		table = commandLine.getOptionValue("table");
		master = commandLine.getOptionValue("master");
	}

	@Override
	public final void execute() throws Exception {
		// show drop command help
		if (showHelp) {
			showHelp();
			return;
		}

		DatabaseResource masterDB = null;
		Connection masterConn = null;
		UserTransaction utx = null;

		try {
			// get master server connection
			masterDB = new DatabaseResource(master);
			masterConn = masterDB.getConnection();

			// begin transaction
			utx = masterDB.getUserTransaction();
			utx.begin();

			// drop
			SyncDatabaseDAO.dropMlog(masterConn, schema, table);

			// commit transaction
			utx.commit();

		} catch (Exception e) {
			// rollback transaction
			if (utx != null && utx.getStatus() != Status.STATUS_NO_TRANSACTION) {
				utx.rollback();
			}
			log.debug(e.getMessage());

			throw e;
		} finally {
			// release resources
			if (masterConn != null) {
				masterConn.close();
			}
			if (masterDB != null) {
				masterDB.stop();
			}
		}

		log.info(mProperty.getMessage("info.drop.success"));
	}

	/* get drop command options */
	@SuppressWarnings("static-access")
	public static Options getOptions() {
		Options options = new Options();
		options.addOption(null, "help", false, "show help");
		options.addOption(OptionBuilder.withLongOpt("master").withDescription(
				"master server name").hasArgs(1).withArgName(
				"master server name").create());
		options.addOption(OptionBuilder.withLongOpt("schema").withDescription(
				"master schema name").hasArgs(1).withArgName("schema name")
				.create());
		options.addOption(OptionBuilder.withLongOpt("table").withDescription(
				"master table name").hasArgs(1).withArgName("table name")
				.create());

		return options;
	}

	/* show drop command help */
	public static void showHelp() {
		Options options = getOptions();
		HelpFormatter f = new HelpFormatter();
		f.printHelp("SyncDatabase drop", options);
	}
}
