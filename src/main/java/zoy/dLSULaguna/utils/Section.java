package zoy.dLSULaguna.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

enum Strand {
    Stem,
    // Extra strands for future proofing
    Abm,
    Humss,
    Adt,
    Spt;

    private static final Map<String, Strand> stringToStrand = new HashMap<>();

    static {
        for (final var strand : Strand.values()) {
            stringToStrand.put(strand.toString(), strand);
        }
    }

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }

    public static Optional<Strand> fromString(String strandString) {
        return Optional.ofNullable(stringToStrand.get(strandString.toUpperCase()));
    }
}

enum Grade {
    Eleven,
    // Extra grade 12 for future proofing
    Twelve;

    private static final Map<String, Grade> stringToGrade = new HashMap<>();

    @Override
    public String toString() {
        return switch (this) {
            case Eleven -> "11";
            case Twelve -> "12";
        };
    }

    static {
        for (final var grade : Grade.values()) {
            stringToGrade.put(grade.toString(), grade);
        }
    }

    public static Optional<Grade> fromString(String gradeString) {
        return Optional.ofNullable(stringToGrade.get(gradeString));
    }
}

enum Letter {
    A,
    B,
    C,
    D,
    E;

    private static final Map<String, Letter> stringToLetter = new HashMap<>();

    static {
        for (final var letter : Letter.values()) {
            stringToLetter.put(letter.toString(), letter);
        }
    }

    public static Optional<Letter> fromString(String letterString) {
        return Optional.ofNullable(stringToLetter.get(letterString));
    }
}

public record Section(Strand strand, Grade grade, Letter letter) {

    public static final Section STEM11_A = new Section(Strand.Stem, Grade.Eleven, Letter.A);
    public static final Section STEM11_B = new Section(Strand.Stem, Grade.Eleven, Letter.B);
    public static final Section STEM11_C = new Section(Strand.Stem, Grade.Eleven, Letter.C);
    public static final Section STEM11_D = new Section(Strand.Stem, Grade.Eleven, Letter.D);
    public static final Section STEM11_E = new Section(Strand.Stem, Grade.Eleven, Letter.E);
    public static final Section STEM12_A = new Section(Strand.Stem, Grade.Twelve, Letter.A);
    public static final Section STEM12_B = new Section(Strand.Stem, Grade.Twelve, Letter.B);
    public static final Section STEM12_C = new Section(Strand.Stem, Grade.Twelve, Letter.C);
    public static final Section STEM12_D = new Section(Strand.Stem, Grade.Twelve, Letter.D);
    public static final Section STEM12_E = new Section(Strand.Stem, Grade.Twelve, Letter.E);
    public static final Section ABM11_A = new Section(Strand.Abm, Grade.Eleven, Letter.A);
    public static final Section ABM11_B = new Section(Strand.Abm, Grade.Eleven, Letter.B);
    public static final Section ABM11_C = new Section(Strand.Abm, Grade.Eleven, Letter.C);
    public static final Section ABM11_D = new Section(Strand.Abm, Grade.Eleven, Letter.D);
    public static final Section ABM11_E = new Section(Strand.Abm, Grade.Eleven, Letter.E);
    public static final Section ABM12_A = new Section(Strand.Abm, Grade.Twelve, Letter.A);
    public static final Section ABM12_B = new Section(Strand.Abm, Grade.Twelve, Letter.B);
    public static final Section ABM12_C = new Section(Strand.Abm, Grade.Twelve, Letter.C);
    public static final Section ABM12_D = new Section(Strand.Abm, Grade.Twelve, Letter.D);
    public static final Section ABM12_E = new Section(Strand.Abm, Grade.Twelve, Letter.E);
    public static final Section HUMSS11_A = new Section(Strand.Humss, Grade.Eleven, Letter.A);
    public static final Section HUMSS11_B = new Section(Strand.Humss, Grade.Eleven, Letter.B);
    public static final Section HUMSS11_C = new Section(Strand.Humss, Grade.Eleven, Letter.C);
    public static final Section HUMSS11_D = new Section(Strand.Humss, Grade.Eleven, Letter.D);
    public static final Section HUMSS11_E = new Section(Strand.Humss, Grade.Eleven, Letter.E);
    public static final Section HUMSS12_A = new Section(Strand.Humss, Grade.Twelve, Letter.A);
    public static final Section HUMSS12_B = new Section(Strand.Humss, Grade.Twelve, Letter.B);
    public static final Section HUMSS12_C = new Section(Strand.Humss, Grade.Twelve, Letter.C);
    public static final Section HUMSS12_D = new Section(Strand.Humss, Grade.Twelve, Letter.D);
    public static final Section HUMSS12_E = new Section(Strand.Humss, Grade.Twelve, Letter.E);
    public static final Section ADT11_A = new Section(Strand.Adt, Grade.Eleven, Letter.A);
    public static final Section ADT11_B = new Section(Strand.Adt, Grade.Eleven, Letter.B);
    public static final Section ADT11_C = new Section(Strand.Adt, Grade.Eleven, Letter.C);
    public static final Section ADT11_D = new Section(Strand.Adt, Grade.Eleven, Letter.D);
    public static final Section ADT11_E = new Section(Strand.Adt, Grade.Eleven, Letter.E);
    public static final Section ADT12_A = new Section(Strand.Adt, Grade.Twelve, Letter.A);
    public static final Section ADT12_B = new Section(Strand.Adt, Grade.Twelve, Letter.B);
    public static final Section ADT12_C = new Section(Strand.Adt, Grade.Twelve, Letter.C);
    public static final Section ADT12_D = new Section(Strand.Adt, Grade.Twelve, Letter.D);
    public static final Section ADT12_E = new Section(Strand.Adt, Grade.Twelve, Letter.E);
    public static final Section SPT11_A = new Section(Strand.Spt, Grade.Eleven, Letter.A);
    public static final Section SPT11_B = new Section(Strand.Spt, Grade.Eleven, Letter.B);
    public static final Section SPT11_C = new Section(Strand.Spt, Grade.Eleven, Letter.C);
    public static final Section SPT11_D = new Section(Strand.Spt, Grade.Eleven, Letter.D);
    public static final Section SPT11_E = new Section(Strand.Spt, Grade.Eleven, Letter.E);
    public static final Section SPT12_A = new Section(Strand.Spt, Grade.Twelve, Letter.A);
    public static final Section SPT12_B = new Section(Strand.Spt, Grade.Twelve, Letter.B);
    public static final Section SPT12_C = new Section(Strand.Spt, Grade.Twelve, Letter.C);
    public static final Section SPT12_D = new Section(Strand.Spt, Grade.Twelve, Letter.D);
    public static final Section SPT12_E = new Section(Strand.Spt, Grade.Twelve, Letter.E);

    private final static Pattern sectionPattern = Pattern.compile("(\\w{3,5})(\\d{2})-(\\w)", Pattern.CASE_INSENSITIVE);

    public static Optional<Section> fromString(String sectionString) {
        final Optional<Section> empty = Optional.empty();
        final var matcher = sectionPattern.matcher(sectionString);

        if (!matcher.find()) {
            return empty;
        }

        final var maybeStrand = Strand.fromString(matcher.group(1));
        if (maybeStrand.isEmpty())
            return empty;

        final var maybeGrade = Grade.fromString(matcher.group(2));
        if (maybeGrade.isEmpty())
            return empty;

        final var maybeLetter = Letter.fromString(matcher.group(3));
        if (maybeLetter.isEmpty())
            return empty;

        return Optional.of(new Section(maybeStrand.get(), maybeGrade.get(), maybeLetter.get()));
    }

    public static String toStringOptional(Optional<Section> maybeSection) {
        return maybeSection.map((section) -> section.toString()).orElse("Unknown Section");
    }

    @Override
    public final String toString() {
        return "" + strand + grade + "-" + letter;
    }
}
