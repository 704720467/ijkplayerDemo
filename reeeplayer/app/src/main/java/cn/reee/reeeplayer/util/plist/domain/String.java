package cn.reee.reeeplayer.util.plist.domain;


import cn.reee.reeeplayer.util.plist.Stringer;

/**
 * Represents a simple plist string element. Not to be confused with
 * {@link String}.
 */
public class String extends PListObject implements
		IPListSimpleObject<java.lang.String> {

	protected Stringer str;

	/**
	 * 
	 */
	private static final long serialVersionUID = -8134261357175236382L;

	public String() {
		setType(PListObjectType.STRING);
		str = new Stringer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.xml.plist.domain.IPListSimpleObject#getValue()
	 */
	@Override
	public java.lang.String getValue() {
		return this.str.getBuilder().toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.xml.plist.domain.IPListSimpleObject#setValue
	 * (Object)
	 */
	@Override
	public void setValue(java.lang.String val) {
		str.newBuilder().append(val);
	}

}