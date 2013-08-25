package com.nomachetejuggling.ssl.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

public class LogSet {
	public List<LogEntry> currentLogs = new ArrayList<LogEntry>();
	public List<LogEntry> previousLogs = new ArrayList<LogEntry>();
	public LocalDate previousDate = null;
}