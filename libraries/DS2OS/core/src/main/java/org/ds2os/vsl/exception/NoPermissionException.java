/*
 * Copyright 2012-2013 Marc-Oliver Pahl, Deniz Ugurlu
 *
 * DS2OS is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version. DS2OS is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public License along
 * with DS2OS. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ds2os.vsl.exception;

/**
 * Excpetion used to indicate permission violations when trying to access knowledge in a way that is
 * not allowed for the requesting subject.
 *
 * @author Marc-Oliver Pahl
 */
public class NoPermissionException extends VslException {

    /**
     * Serial version uid.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MAJOR = 4;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MINOR = 3;

    /**
     * @see Exception#Exception(String)
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public NoPermissionException(final String message) {
        super(message);
    }

    /**
     * @see Exception#Exception(Throwable)
     * @param cause
     *            the cause (which is saved for later retrieval by the Throwable.getCause() method).
     *            (A null value is permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public NoPermissionException(final Throwable cause) {
        super(cause);
    }

    @Override
    public final byte getErrorCodeMajor() {
        return ERROR_CODE_MAJOR;
    }

    @Override
    public final byte getErrorCodeMinor() {
        return ERROR_CODE_MINOR;
    }
}
