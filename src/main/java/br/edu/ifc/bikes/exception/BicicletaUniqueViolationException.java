package br.edu.ifc.bikes.exception;

public class BicicletaUniqueViolationException extends RuntimeException {
    public BicicletaUniqueViolationException(String message) {
        super(message);
    }
}
