package dev.xeys.visuals.anim;

/** Easing curves mapping linear progress t in [0, 1] to eased progress. */
public enum Easing {
	LINEAR {
		@Override
		public float apply(float t) {
			return t;
		}
	},
	IN_QUAD {
		@Override
		public float apply(float t) {
			return t * t;
		}
	},
	OUT_QUAD {
		@Override
		public float apply(float t) {
			return 1f - (1f - t) * (1f - t);
		}
	},
	IN_OUT_QUAD {
		@Override
		public float apply(float t) {
			return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
		}
	},
	IN_CUBIC {
		@Override
		public float apply(float t) {
			return t * t * t;
		}
	},
	OUT_CUBIC {
		@Override
		public float apply(float t) {
			float u = 1f - t;
			return 1f - u * u * u;
		}
	},
	IN_OUT_CUBIC {
		@Override
		public float apply(float t) {
			return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
		}
	},
	OUT_QUART {
		@Override
		public float apply(float t) {
			float u = 1f - t;
			return 1f - u * u * u * u;
		}
	},
	OUT_QUINT {
		@Override
		public float apply(float t) {
			float u = 1f - t;
			return 1f - u * u * u * u * u;
		}
	},
	OUT_EXPO {
		@Override
		public float apply(float t) {
			return t >= 1f ? 1f : 1f - (float) Math.pow(2.0, -10.0 * t);
		}
	},
	IN_OUT_EXPO {
		@Override
		public float apply(float t) {
			if (t <= 0f) {
				return 0f;
			}
			if (t >= 1f) {
				return 1f;
			}
			return t < 0.5f ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2f : (2f - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2f;
		}
	},
	IN_OUT_SINE {
		@Override
		public float apply(float t) {
			return -((float) Math.cos(Math.PI * t) - 1f) / 2f;
		}
	},
	OUT_SINE {
		@Override
		public float apply(float t) {
			return (float) Math.sin(t * Math.PI / 2.0);
		}
	},
	OUT_BACK {
		@Override
		public float apply(float t) {
			float c1 = 1.70158f;
			float c3 = c1 + 1f;
			float u = t - 1f;
			return 1f + c3 * u * u * u + c1 * u * u;
		}
	},
	OUT_BACK_SOFT {
		@Override
		public float apply(float t) {
			float c1 = 0.9f;
			float c3 = c1 + 1f;
			float u = t - 1f;
			return 1f + c3 * u * u * u + c1 * u * u;
		}
	},
	OUT_ELASTIC {
		@Override
		public float apply(float t) {
			if (t <= 0f) {
				return 0f;
			}
			if (t >= 1f) {
				return 1f;
			}
			double c4 = (2.0 * Math.PI) / 3.0;
			return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
		}
	};

	public abstract float apply(float t);

	public float apply(float from, float to, float t) {
		return from + (to - from) * this.apply(Math.max(0f, Math.min(1f, t)));
	}
}
