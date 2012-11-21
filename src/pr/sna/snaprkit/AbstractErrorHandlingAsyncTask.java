package pr.sna.snaprkit;

import android.os.AsyncTask;

public abstract class AbstractErrorHandlingAsyncTask<Params, Progress, Result>
		extends AsyncTask<Params, Progress, Object>
{
	protected final Object doInBackground(Params... params)
	{
		try
		{
			return computeResult(params);
		}
		catch (Throwable e)
		{
			return e;
		}
	}

	protected abstract Result computeResult(Params...params) throws Throwable;

	@SuppressWarnings({ "unchecked" })
	protected final void onPostExecute(Object object)
	{
		if (object instanceof Throwable)
		{
			onError((Throwable) object);
		}
		else
		{
			onResult((Result) object);
		}
	}

	protected abstract void onResult(Result result);

	protected abstract void onError(Throwable e);
}