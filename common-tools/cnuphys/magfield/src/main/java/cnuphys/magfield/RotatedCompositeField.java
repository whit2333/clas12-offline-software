package cnuphys.magfield;

public final class RotatedCompositeField extends CompositeField {
	
	@Override
	public String getName() {
		String s = "Rotated Composite contains: ";

		int count = 1;
		for (IMagField field : this) {
			if (count == 1) {
				s += field.getName();
			}
			else {
				s += " + " + field.getName();
			}
			count++;
		}

		return s;
	}
}
