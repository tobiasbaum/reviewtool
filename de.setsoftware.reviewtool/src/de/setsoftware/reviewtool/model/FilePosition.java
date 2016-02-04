package de.setsoftware.reviewtool.model;

public class FilePosition extends Position {

	private final String shortName;

	public FilePosition(String shortName) {
		this.shortName = shortName;
	}

	@Override
	public String serialize() {
		return "(" + this.shortName + ")";
	}

	@Override
	public String getShortFileName() {
		return this.shortName;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FilePosition)) {
			return false;
		}
		final FilePosition p = (FilePosition) o;
		return p.shortName.equals(this.shortName);
	}

	@Override
	public int hashCode() {
		return this.shortName.hashCode();
	}

	@Override
	public String toString() {
		return this.serialize();
	}
}
