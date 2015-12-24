# WakaTimeFreshBooks
A Java-based command line utility to create [FreshBooks](http://www.freshbooks.com) time entries from [WakaTime](http://wakatime.com) submissions.

This tool is designed to pull time logs made available through the WakaTime API and create FreshBooks time entries for them. With a little configuration, it can be used to automatically log your time for you daily. In conjunction with what WakaTime does already, this essentially means effortless time logging! If you wish, you can also run this manually and interactively as well.

## Getting Started
To use WakaTimeFreshBooks, you'll need to download the latest release from the Releases section.  Then, make sure you have the following information handy:

* WakaTime API Key (if you've already configured WakaTime on your machine, WakaTimeFreshBooks can locate it)
* FreshBooks account name (whatever is in {account}.freshbooks.com)
* FreshBooks Authentication Token (Login, Go to Profile > API Access to get this).

## Configuring WakaTimeFreshBooks

To get started, simply run the JAR on your command line:

```{r, engine='shell'}
java -jar w2f.jar
```

You will see a welcome message and be prompted to enter your WakaTime API Key, your FreshBooks account name, and FreshBooks authentication token.

That's all you need to get started.

By default, these configuration options will be stored in ~/.w2f.properties, and a running log of activity will be stored in ~/.w2f.log.  See [Generated Files](#generated-files) section for more details on these files.

### Associating Projects

You will be prompted to associate your WakaTime projects with FreshBooks projects in the initialization step, but you can do this at any time by running:

```{r, engine='shell'}
java -jar w2f.jar --add-project
```

Associating a project is very easy. Run the command above, then make the three necessary selections when prompted:

* Choose a WakaTime project
* Choose a FreshBooks project
* Choose a FreshBooks task

And that's it. Any time recorded in that WakaTime project will be logged to FreshBooks using the project and task specified.

Note: you can remove a project at any time from the association list using:

```{r, engine='shell'}
java -jar w2f.jar --remove-project
```

### Viewing Time Entries

Once you have configured WakaTimeFreshBooks, you can run the utility in view-only mode to see what is available to log:

```{r, engine='shell'}
java -jar w2f.jar --dry-run
```

You can also run this without any arguments, as view-only is the default mode.

```{r, engine='shell'}
java -jar w2f.jar
```

The date of the time logs defaults to today, but you can pass a flag to override this:

```{r, engine='shell'}
java -jar w2f.jar -d {yyyy-MM-dd}
```

### Submitting Time Entries

To send your WakaTime entries to FreshBooks, run with the `--submit` command:

```{r, engine='shell'}
java -jar w2f.jar --submit
```

All of your time logs for that day that have associated projects will be immediately submitted as time entries to FreshBooks.

To choose which time entries should be sent, and associate notes with these logs, run in interactive mode:

```{r, engine='shell'}
java -jar w2f.jar --submit --interactive
```

For each time entry, you will be prompted as to whether or not you want to log it or not, and you'll have the option to enter notes to send along with the time entry.

### Running in the background

To really make this automated, you can elect to have this run as a cron job, meaning that your time can be logged automatically for you daily.  To set this up, enter a line like so in your crontab:

```
0	22	*	*	*	java -jar /path/to/w2f.jar --timestamps --submit
```

This will submit your time entries at the end of the day each day.  The `--timestamps` flag will add start and end timestamps to the log (details below).

You can also throw in the `--verbose` flag to add more output to the running log file, if you plan to review it later.

## Generated Files

Two files are stored on your local machine to make WakaTimeFreshBooks work, a configuration file and a log file. By default, these are both stored in your home directory.

### .w2f.properties

The properties file stores your settings about projects as well as your API keys. If you don't like/don't want to use the default location, you can specify the location you prefer:

```{r, engine='shell'}
java -jar w2f.jar -c /my/path/to/w2f.cfg
```

You should never have to edit this file directly, but it's nice to know where it is.

### .w2f.log

The log contains information about what was logged and, when verbose mode is used, what was not logged.  Like the config file, you can select the location of this file on a per-run basis:

```{r, engine='shell'}
java -jar w2f.jar -l /my/path/to/w2f.log
```

By default, this file will only store a maximum of 1000 lines. To change, this, run:

```{r, engine='shell'}
java -jar w2f.jar --set-event-log-size={num_lines}
```

## Additional Usage Options

You can view all the options available to you by running:

```{r, engine='shell'}
java -jar w2f.jar --usage
```


## Resources
* FreshBooks: https://www.freshbooks.com
* WakaTime: https://wakatime.com

Issue? Suggestion? File a ticket here on GitHub, submit a PR, or tweet me @GoGoCarl.
