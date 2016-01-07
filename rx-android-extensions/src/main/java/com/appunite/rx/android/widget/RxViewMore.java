package com.appunite.rx.android.widget;

import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.view.View;
import android.view.ViewTreeObserver;

import com.appunite.rx.Size;
import com.appunite.rx.android.internal.MainThreadSubscription;
import com.appunite.rx.android.internal.Preconditions;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.appunite.rx.internal.Preconditions.checkNotNull;

public class RxViewMore {

    @Nonnull
    public static Action1<? super Number> translateX(@Nonnull final View view) {
        checkNotNull(view);
        return new Action1<Number>() {
            @Override
            public void call(final Number number) {
                ViewCompat.setTranslationX(view, number.floatValue());
            }
        };
    }

    @Nonnull
    public static Action1<? super Number> translateY(@Nonnull final View view) {
        checkNotNull(view);
        return new Action1<Number>() {
            @Override
            public void call(final Number number) {
                ViewCompat.setTranslationY(view, number.floatValue());
            }
        };
    }

    @Nonnull
    public static Action1<? super Number> alpha(@Nonnull final View view) {
        checkNotNull(view);
        return new Action1<Number>() {
            @Override
            public void call(final Number number) {
                ViewCompat.setAlpha(view, number.floatValue());
            }
        };
    }

    @Nonnull
    public static Observable<Integer> width(@Nonnull final View view) {
        return size(view)
                .map(new Func1<ViewSize, Integer>() {
                    @Override
                    public Integer call(ViewSize viewSize) {
                        return viewSize.width();
                    }
                })
                .distinctUntilChanged();
    }

    @Nonnull
    public static Observable<Integer> height(@Nonnull final View view) {
        return size(view)
                .map(new Func1<ViewSize, Integer>() {
                    @Override
                    public Integer call(ViewSize viewSize) {
                        return viewSize.height();
                    }
                })
                .distinctUntilChanged();
    }

    @Nonnull
    public static Observable<ViewSize> size(@Nonnull final View view) {
        return Observable.create(new OnViewSize(view))
                .distinctUntilChanged();
    }

    public static class ViewSize extends Size {

        @Nonnull
        private final View view;

        public ViewSize(@Nonnull View view, int width, int height) {
            super(width, height);
            this.view = view;
        }

        @Nonnull
        public View view() {
            return view;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ViewSize)) return false;
            if (!super.equals(o)) return false;

            final ViewSize viewSize = (ViewSize) o;

            return view.equals(viewSize.view);

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + view.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ViewSize{" +
                    "view=" + view +
                    '}';
        }
    }

    private static class OnViewSize implements Observable.OnSubscribe<ViewSize> {

        private View view;

        public OnViewSize(@Nonnull View view) {
            this.view = view;
        }

        @Override
        public void call(final Subscriber<? super ViewSize> subscriber) {
            Preconditions.checkUiThread();
            sendSizeIfValid(subscriber);

            final ViewTreeObserver.OnPreDrawListener listener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    sendSizeIfValid(subscriber);
                    return true;
                }
            };
            view.getViewTreeObserver().addOnPreDrawListener(listener);

            subscriber.add(new MainThreadSubscription() {
                @Override protected void onUnsubscribe() {
                    view.getViewTreeObserver().removeOnPreDrawListener(listener);
                }
            });
        }

        protected void sendSizeIfValid(Subscriber<? super ViewSize> subscriber) {
            final int width = view.getWidth();
            final int height = view.getHeight();
            if (width > 0 && height > 0 && !view.isLayoutRequested()) {
                subscriber.onNext(new ViewSize(view, width, height));
            }
        }
    }

    @Nonnull
    public static Observable<Integer> statusBarHeight(@Nonnull final View view) {
        return onApplyWindowInsets(view)
                .map(new Func1<WindowInsetsCompat, Integer>() {
                    @Override
                    public Integer call(final WindowInsetsCompat windowInsetsCompat) {
                        return windowInsetsCompat.getSystemWindowInsetTop();
                    }
                })
                .distinctUntilChanged();
    }

    @Nonnull
    public static Observable<Integer> navigationBarHeight(@Nonnull final View view) {
        return onApplyWindowInsets(view)
                .map(new Func1<WindowInsetsCompat, Integer>() {
                    @Override
                    public Integer call(final WindowInsetsCompat windowInsetsCompat) {
                        return windowInsetsCompat.getSystemWindowInsetBottom();
                    }
                })
                .distinctUntilChanged();
    }

    @Nonnull
    public static Observable<WindowInsetsCompat> onApplyWindowInsets(@Nonnull final View view) {
        return Observable.create(new OnApplyWindowInsets(view))
                .distinctUntilChanged();
    }

    private static class OnApplyWindowInsets implements Observable.OnSubscribe<WindowInsetsCompat> {

        private View view;

        public OnApplyWindowInsets(@Nonnull View view) {
            this.view = view;
        }

        @Override
        public void call(final Subscriber<? super WindowInsetsCompat> subscriber) {
            Preconditions.checkUiThread();

            ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(final View v, final WindowInsetsCompat insets) {
                    subscriber.onNext(insets);
                    return insets;
                }
            });

            subscriber.add(new MainThreadSubscription() {
                @Override protected void onUnsubscribe() {
                    ViewCompat.setOnApplyWindowInsetsListener(view, null);
                }
            });
        }
    }
}
