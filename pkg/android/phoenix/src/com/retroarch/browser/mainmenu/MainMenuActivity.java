package com.retroarch.browser.mainmenu;

import com.agr541.retroarch.BuildConfig;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.retroactivity.RetroActivityFuture;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.net.Uri;

import java.util.List;
import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.Manifest;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.util.Log;

/**
 * {@link PreferenceActivity} subclass that provides all of the
 * functionality of the main menu screen.
 */
public final class MainMenuActivity extends PreferenceActivity
{
	final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
	final private int REQUEST_CODE_MANAGE_STORAGE = 125;
	public static String PACKAGE_NAME;
	boolean checkPermissions = false;

	public void showMessageOKCancel(String message, DialogInterface.OnClickListener onClickListener)
	{
		new AlertDialog.Builder(this).setMessage(message)
			.setPositiveButton("OK", onClickListener).setCancelable(false)
			.setNegativeButton("Cancel", null).create().show();
	}

	private boolean addPermission(List<String> permissionsList, String permission)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
			{
				permissionsList.add(permission);

				// Check for Rationale Option
				if (!shouldShowRequestPermissionRationale(permission))
					return false;
			}
		}

		return true;
	}

	public void checkRuntimePermissions()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		{
			if (!Environment.isExternalStorageManager())
			{
				checkPermissions = true;
				showMessageOKCancel("RetroArch needs 'All Files Access' to manage your cores and game data. Please grant this permission on the next screen.",
					new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if (which == AlertDialog.BUTTON_POSITIVE)
							{
								try
								{
									Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
									intent.addCategory("android.intent.category.DEFAULT");
									intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
									startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
								}
								catch (Exception e)
								{
									Intent intent = new Intent();
									intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
									startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
								}
							}
						}
					});
				return;
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			List<String> permissionsNeeded = new ArrayList<String>();
			final List<String> permissionsList = new ArrayList<String>();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			{
				if (!addPermission(permissionsList, Manifest.permission.READ_MEDIA_IMAGES))
					permissionsNeeded.add("Images");
				if (!addPermission(permissionsList, Manifest.permission.READ_MEDIA_VIDEO))
					permissionsNeeded.add("Video");
				if (!addPermission(permissionsList, Manifest.permission.READ_MEDIA_AUDIO))
					permissionsNeeded.add("Audio");
			}
			else
			{
				if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
					permissionsNeeded.add("Read External Storage");
				if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
					permissionsNeeded.add("Write External Storage");
			}

			if (permissionsList.size() > 0)
			{
				checkPermissions = true;

				String message = "You need to grant access to " + permissionsNeeded.get(0);
				for (int i = 1; i < permissionsNeeded.size(); i++)
					message = message + ", " + permissionsNeeded.get(i);

				showMessageOKCancel(message,
					new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if (which == AlertDialog.BUTTON_POSITIVE)
							{
								requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
										REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
							}
						}
					});
			}
		}

		if (!checkPermissions)
		{
			finalStartup();
		}
	}

	public void finalStartup()
	{
		Intent retro = new Intent(this, RetroActivityFuture.class);

		if (RetroActivityFuture.isRunning) {
			retro.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		} else {
			retro.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

			startRetroActivity(
					retro,
					null,
					prefs.getString("libretro_path", getApplicationInfo().dataDir + "/cores/"),
					UserPreferences.getDefaultConfigPath(this),
					Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD),
					getApplicationInfo().dataDir,
					getApplicationInfo().sourceDir);
		}

		startActivity(retro);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_CODE_MANAGE_STORAGE)
		{
			checkRuntimePermissions();
		}
		else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
				for (int i = 0; i < permissions.length; i++)
				{
					if(grantResults[i] == PackageManager.PERMISSION_GRANTED)
					{
						Log.i("MainMenuActivity", "Permission: " + permissions[i] + " was granted.");
					}
					else
					{
						Log.i("MainMenuActivity", "Permission: " + permissions[i] + " was not granted.");
					}
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
				break;
		}

		finalStartup();
	}

	public static void startRetroActivity(Intent retro, String contentPath, String corePath,
			String configFilePath, String imePath, String dataDirPath, String dataSourcePath)
	{
		if (contentPath != null) {
			retro.putExtra("ROM", contentPath);
		}
		retro.putExtra("LIBRETRO", corePath);
		retro.putExtra("CONFIGFILE", configFilePath);
		retro.putExtra("IME", imePath);
		retro.putExtra("DATADIR", dataDirPath);
		retro.putExtra("APK", dataSourcePath);
		String external = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + PACKAGE_NAME + "/files";
		retro.putExtra("SDCARD", BuildConfig.PLAY_STORE_BUILD ? external : Environment.getExternalStorageDirectory().getAbsolutePath());
		retro.putExtra("EXTERNAL", external);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		PACKAGE_NAME = getPackageName();

		// Bind audio stream to hardware controls.
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		UserPreferences.updateConfigFile(this);

		if (BuildConfig.PLAY_STORE_BUILD)
			finalStartup();
		else
			checkRuntimePermissions();
	}
}
