package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A button in the settings panel (not persisted), e.g. "Reset positions". */
public final class ActionSetting extends Setting<Boolean> {
	private final Runnable action;
	private final String buttonLabel;

	public ActionSetting(String key, String name, String buttonLabel, Runnable action) {
		super(key, name, Boolean.FALSE);
		this.buttonLabel = buttonLabel;
		this.action = action;
	}

	public String buttonLabel() {
		return this.buttonLabel;
	}

	public void run() {
		this.action.run();
	}

	@Override
	public JsonElement toJson() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void fromJson(JsonElement json) {
	}
}
