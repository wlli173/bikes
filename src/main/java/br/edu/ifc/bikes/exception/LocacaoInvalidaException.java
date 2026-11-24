package br.edu.ifc.bikes.exception;

public class LocacaoInvalidaException extends RuntimeException {
    public LocacaoInvalidaException(String message) {
        super(message);
    }
}
