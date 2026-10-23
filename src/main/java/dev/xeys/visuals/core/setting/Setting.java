package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * A configurable value on a module. Settings are declared once, persist to JSON under a stable key, can
 * be conditionally visible (e.g. "Glow color" only while glow is on) and notify listeners on change.
 */
public abstract class Setting<T> {
	private final String key;
	private final String name;
	private String description = "";
	private @Nullable String group;
	private BooleanSupplier visible = () -> true;
	private final List<Consumer<T>> listeners = new ArrayList<>(1);
	protected final T defaultValue;
	protected T value;

	protected Setting(String key, String name, T defaultValue) {
		this.key = key;
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String key() {
		return this.key;
	}

	public String name() {
		return this.name;
	}

	public String description() {
		return this.description;
	}

	public @Nullable String group() {
		return this.group;
	}

	public boolean isVisible() {
		return this.visible.getAsBoolean();
	}

	public T get() {
		return this.value;
	}

	public T defaultValue() {
		return this.defaultValue;
	}

	public void set(T newValue) {
		T clamped = this.sanitize(newValue);
		if (!clamped.equals(this.value)) {
			this.value = clamped;
			for (int i = 0; i < this.listeners.size(); i++) {
				this.listeners.get(i).accept(clamped);
			}
			SettingChanges.markDirty();
		}
	}

	public void reset() {
		this.set(this.defaultValue);
	}

	public boolean isDefault() {
		return this.value.equals(this.defaultValue);
	}

	/** Coerces a value into the setting's valid range. */
	protected T sanitize(T v) {
		return v;
	}

	// ------------------------------------------------------------------ fluent configuration

	@SuppressWarnings("unchecked")
	private <S extends Setting<T>> S self() {
		return (S) this;
	}

	public <S extends Setting<T>> S describe(String text) {
		this.description = text;
		return this.self();
	}

	public <S extends Setting<T>> S group(String name) {
		this.group = name;
		return this.self();
	}

	public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
		this.visible = condition;
		return this.self();
	}

	public <S extends Setting<T>> S onChange(Consumer<T> listener) {
		this.listeners.add(listener);
		return this.self();
	}

	// ------------------------------------------------------------------ persistence

	public abstract JsonElement toJson();

	/** Applies a stored value; malformed data falls back to the default rather than failing the load. */
	public abstract void fromJson(JsonElement json);
}
