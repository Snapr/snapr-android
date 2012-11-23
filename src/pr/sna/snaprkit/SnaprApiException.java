package pr.sna.snaprkit;

public class SnaprApiException extends RuntimeException
{
	private static final long serialVersionUID = -5263313855587580161L;

	private String mType;
	private String mMessage;
	
	public String getType()
	{
		return mType;
	}

	@Override
	public String getMessage() {
		return mMessage;
	}
	
	public SnaprApiException(String type, String message)
	{
		this.mType = type;
		this.mMessage = message;
	}
}
