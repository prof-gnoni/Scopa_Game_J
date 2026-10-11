package scopa.shared;

public enum Seme {
    BASTONI, COPPE, DENARI, SPADE;

    public String getNomeFile() {
        return this.name().toLowerCase();
    }
}