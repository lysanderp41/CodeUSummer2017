package codeu.chat.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public final class Interests {

  public static final Serializer<Interests> SERIALIZER = new Serializer<Interests>() {

    @Override
    public void write(OutputStream out, Interests value) throws IOException {

      Serializers.collection(Uuid.SERIALIZER).write(out, value.interests);
      Time.SERIALIZER.write(out, value.lastStatusUpdate);
      Time.SERIALIZER.write(out, value.creation);

    }

    @Override
    public Interests read(InputStream in) throws IOException {

      return new Interests(
          Serializers.collection(Uuid.SERIALIZER).read(in),
          Time.SERIALIZER.read(in),
          Time.SERIALIZER.read(in)
      );

    }
  };

  public final Collection<Uuid> interests;
  public Time lastStatusUpdate;
  public final Time creation;

  public Interests(Collection<Uuid> interests, Time lastStatusUpdate, Time creation) {

    this.interests = interests;
    this.lastStatusUpdate = lastStatusUpdate;
    this.creation = creation;

  }
}