package br.edu.ifc.bikes.exception;

public class BicicletaIndisponivelException extends RuntimeException {
    public BicicletaIndisponivelException(String message) {
        super(message);
    }
}
