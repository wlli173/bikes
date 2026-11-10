package br.edu.ifc.bikes.exception;

public class ClienteUniqueViolationException extends RuntimeException {
    public ClienteUniqueViolationException(String message) {
        super(message);
    }
}
