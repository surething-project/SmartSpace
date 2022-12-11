/*
 * Copyright (C) 2020 The SureThing project
 * @author João Tiago <joao.marques.tiago@tecnico.ulisboa.pt>
 * http://surething.tecnico.ulisboa.pt/en/
 */

package pt.ulisboa.tecnico.surespace.common.manager;

import pt.ulisboa.tecnico.surespace.common.manager.exception.LogManagerException;

public interface LogManagerInterface extends ManagerInterface<LogManagerException> {
  void debug(Object message);

  default void debug(String format, Object... args) {
    debug(getFormattedString(format, args));
  }

  void error(Object message);

  default void error(String format, Object... args) {
    error(getFormattedString(format, args));
  }

  default String getFormattedString(String format, Object... args) {
    return String.format(format, args);
  }

  void info(Object message);

  default void info(String format, Object... args) {
    info(getFormattedString(format, args));
  }

  void warning(Object message);

  default void warning(String format, Object... args) {
    warning(getFormattedString(format, args));
  }
}
