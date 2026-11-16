package tokyo.ghoul.api.utils.rotation.neuro;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OnnxWriter {
    private static final int IR_VERSION = 8;
    private static final int OPSET = 14;
    private static final int FLOAT = 1;
    private static final int INT64 = 7;
    private static final int ATTR_INT = 2;

    private final List<byte[]> nodes = new ArrayList<>();
    private final List<byte[]> initializers = new ArrayList<>();
    private final List<byte[]> inputs = new ArrayList<>();
    private final List<byte[]> outputs = new ArrayList<>();
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private int nodeCounter;

    public OnnxWriter gemm(String input, String output, float[][] weights, float[] bias) {
        int in = weights.length;
        int out = weights[0].length;
        float[] flat = new float[in * out];
        for (int i = 0; i < in; i++) {
            System.arraycopy(weights[i], 0, flat, i * out, out);
        }
        String w = output + "_W";
        String b = output + "_b";
        initializers.add(floatTensor(w, new long[]{in, out}, flat));
        initializers.add(floatTensor(b, new long[]{out}, bias));
        nodes.add(node("Gemm", new String[]{input, w, b}, new String[]{output}, Map.of()));
        return this;
    }

    public OnnxWriter add(String a, String b, String output) {
        nodes.add(node("Add", new String[]{a, b}, new String[]{output}, Map.of()));
        return this;
    }

    public OnnxWriter tanh(String input, String output) {
        nodes.add(node("Tanh", new String[]{input}, new String[]{output}, Map.of()));
        return this;
    }

    public OnnxWriter gru(String input, String initialH, String outputY, String outputH,
                          float[][] w, float[][] r, float[] bias) {
        int gates = w.length;
        int in = w[0].length;
        int hidden = gates / 3;
        float[] flatW = new float[gates * in];
        float[] flatR = new float[gates * hidden];
        for (int i = 0; i < gates; i++) {
            System.arraycopy(w[i], 0, flatW, i * in, in);
            System.arraycopy(r[i], 0, flatR, i * hidden, hidden);
        }
        float[] b = new float[gates * 2];
        System.arraycopy(bias, 0, b, 0, gates);

        String prefix = outputH + "_";
        initializers.add(floatTensor(prefix + "W", new long[]{1, gates, in}, flatW));
        initializers.add(floatTensor(prefix + "R", new long[]{1, gates, hidden}, flatR));
        initializers.add(floatTensor(prefix + "B", new long[]{1, gates * 2}, b));
        nodes.add(node("GRU", new String[]{input, prefix + "W", prefix + "R", prefix + "B", "", initialH},
                new String[]{outputY, outputH}, Map.of("hidden_size", (long) hidden)));
        return this;
    }

    public OnnxWriter reshape(String input, String output, long... shape) {
        String shapeName = output + "_shape";
        initializers.add(int64Tensor(shapeName, shape));
        nodes.add(node("Reshape", new String[]{input, shapeName}, new String[]{output}, Map.of()));
        return this;
    }

    public OnnxWriter input(String name, Object... dims) {
        inputs.add(valueInfo(name, dims));
        return this;
    }

    public OnnxWriter output(String name, Object... dims) {
        outputs.add(valueInfo(name, dims));
        return this;
    }

    public OnnxWriter meta(String key, String value) {
        metadata.put(key, value);
        return this;
    }

    public byte[] build() {
        Proto graph = new Proto();
        for (byte[] n : nodes) graph.message(1, n);
        graph.string(2, "neuro_rotation");
        for (byte[] t : initializers) graph.message(5, t);
        for (byte[] i : inputs) graph.message(11, i);
        for (byte[] o : outputs) graph.message(12, o);

        Proto opset = new Proto();
        opset.int64(2, OPSET);

        Proto model = new Proto();
        model.int64(1, IR_VERSION);
        model.string(2, "desire");
        model.string(3, "2");
        model.message(7, graph.bytes());
        model.message(8, opset.bytes());
        for (Map.Entry<String, String> e : metadata.entrySet()) {
            Proto entry = new Proto();
            entry.string(1, e.getKey());
            entry.string(2, e.getValue());
            model.message(14, entry.bytes());
        }
        return model.bytes();
    }

    private byte[] node(String op, String[] in, String[] out, Map<String, Long> intAttributes) {
        Proto node = new Proto();
        for (String i : in) node.string(1, i);
        for (String o : out) node.string(2, o);
        node.string(3, op.toLowerCase() + "_" + nodeCounter++);
        node.string(4, op);
        for (Map.Entry<String, Long> attr : intAttributes.entrySet()) {
            Proto a = new Proto();
            a.string(1, attr.getKey());
            a.int64(3, attr.getValue());
            a.int64(20, ATTR_INT);
            node.message(5, a.bytes());
        }
        return node.bytes();
    }

    private static byte[] floatTensor(String name, long[] dims, float[] data) {
        ByteBuffer raw = ByteBuffer.allocate(data.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : data) raw.putFloat(v);
        return tensor(name, dims, FLOAT, raw.array());
    }

    private static byte[] int64Tensor(String name, long[] data) {
        ByteBuffer raw = ByteBuffer.allocate(data.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (long v : data) raw.putLong(v);
        return tensor(name, new long[]{data.length}, INT64, raw.array());
    }

    private static byte[] tensor(String name, long[] dims, int type, byte[] raw) {
        Proto tensor = new Proto();
        for (long d : dims) tensor.int64(1, d);
        tensor.int64(2, type);
        tensor.string(8, name);
        tensor.message(9, raw);
        return tensor.bytes();
    }

    private static byte[] valueInfo(String name, Object... dims) {
        Proto shape = new Proto();
        for (Object dim : dims) {
            Proto d = new Proto();
            if (dim instanceof String param) d.string(2, param);
            else d.int64(1, ((Number) dim).longValue());
            shape.message(1, d.bytes());
        }

        Proto tensorType = new Proto();
        tensorType.int64(1, FLOAT);
        tensorType.message(2, shape.bytes());

        Proto type = new Proto();
        type.message(1, tensorType.bytes());

        Proto info = new Proto();
        info.string(1, name);
        info.message(2, type.bytes());
        return info.bytes();
    }

    private static final class Proto {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        void int64(int field, long value) {
            varint(((long) field << 3));
            varint(value);
        }

        void string(int field, String value) {
            message(field, value.getBytes(StandardCharsets.UTF_8));
        }

        void message(int field, byte[] payload) {
            varint(((long) field << 3) | 2);
            varint(payload.length);
            out.writeBytes(payload);
        }

        private void varint(long value) {
            while ((value & ~0x7FL) != 0) {
                out.write((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
            out.write((int) value);
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }
}
