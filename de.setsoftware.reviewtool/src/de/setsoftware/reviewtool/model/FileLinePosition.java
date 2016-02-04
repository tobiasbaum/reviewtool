package de.setsoftware.reviewtool.model;

public class FileLinePosition extends Position {

	private final String shortName;
	private final int line;

	public FileLinePosition(String shortName, int line) {
		this.shortName = shortName;
		this.line = line;
	}

	@Override
	public String serialize() {
		return "(" + this.shortName + ", " + this.line + ")";
	}

	public int getLine() {
		return this.line;
	}

	@Override
	public String getShortFileName() {
		return this.shortName;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FileLinePosition)) {
			return false;
		}
		final FileLinePosition p = (FileLinePosition) o;
		return p.shortName.equals(this.shortName)
				&& p.line == this.line;
	}

	@Override
	public int hashCode() {
		return this.shortName.hashCode() ^ this.line;
	}

	@Override
	public String toString() {
		return this.serialize();
	}

}
