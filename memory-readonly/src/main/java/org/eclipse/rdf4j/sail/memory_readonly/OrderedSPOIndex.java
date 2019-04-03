/* @formatter:off */
/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory_readonly;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class OrderedSPOIndex {

	private static final ArrayIndexIterable.EmptyArrayIndexIterable EMPTY_ARRAY_INDEX_ITERABLE = new ArrayIndexIterable.EmptyArrayIndexIterable();

	final Statement[] orderedArray;

	final Map<SpoCompound, ArrayIndex> spoIndex;
	final Map<SpCompound, ArrayIndex> spIndex;
	final Map<SCompound, ArrayIndex> sIndex;

	private static final ValueComparator valueComparator = new ValueComparator();

	OrderedSPOIndex(Set<Statement> statementSet) {

		spoIndex = new HashMap<>(statementSet.size()/5, 0.5f);
		spIndex = new HashMap<>(statementSet.size()/5, 0.5f);
		sIndex = new HashMap<>(statementSet.size()/5, 0.5f);


		orderedArray = statementSet.stream()
			.sorted(getStatementComparator())
			.toArray(Statement[]::new);


		for (int i = 0; i < orderedArray.length; i++) {
			Statement statement = orderedArray[i];

			int index = i;

			SCompound sKey = new SCompound(statement.getSubject());
			SpCompound spKey = new SpCompound(statement.getSubject(), statement.getPredicate());
			SpoCompound spoKey = new SpoCompound(statement.getSubject(), statement.getPredicate(),
				statement.getObject());

			sIndex.compute(sKey, (key, value) -> {
				if (value == null) {
					return new ArrayIndex(index, index + 1);
				} else {
					value.stopExclusive = index + 1;
					return value;
				}
			});

			spIndex.compute(spKey, (key, value) -> {
				if (value == null) {
					return new ArrayIndex(index, index + 1);
				} else {
					value.stopExclusive = index + 1;
					return value;
				}
			});

			spoIndex.compute(spoKey, (key, value) -> {
				if (value == null) {
					return new ArrayIndex(index, index + 1);
				} else {
					value.stopExclusive = index + 1;
					return value;
				}
			});

		}

	}

	ArrayIndexIterable getStatements(Resource subject, IRI predicate, Value object, Resource... context) {

		if (subject != null) {
			if (predicate != null) {
				if (object != null) {
					ArrayIndex arrayIndex = spoIndex.get(new SpoCompound(subject, predicate, object));
					if (arrayIndex == null) {
						return EMPTY_ARRAY_INDEX_ITERABLE;
					}

					if ((context == null || context.length == 0)) {
						return new ArrayIndexIterable(orderedArray, arrayIndex.startInclusive, arrayIndex.stopExclusive,
							false);
					} else {
						return new ArrayIndexIterable(orderedArray, arrayIndex.startInclusive, arrayIndex.stopExclusive,
							true);
					}

				} else {
					ArrayIndex arrayIndex = spIndex.get(new SpCompound(subject, predicate));
					if (arrayIndex == null) {
						return EMPTY_ARRAY_INDEX_ITERABLE;
					}

					if ((context == null || context.length == 0)) {
						return new ArrayIndexIterable(orderedArray, arrayIndex.startInclusive, arrayIndex.stopExclusive,
							false);
					} else {
						return new ArrayIndexIterable(orderedArray, arrayIndex.startInclusive, arrayIndex.stopExclusive,
							true);
					}
				}

			} else {
				ArrayIndex arrayIndex = sIndex.get(new SCompound(subject));
				if (arrayIndex == null) {
					return EMPTY_ARRAY_INDEX_ITERABLE;
				}

				if (object == null && (context == null || context.length == 0)) {
					return new ArrayIndexIterable(orderedArray, arrayIndex.startInclusive, arrayIndex.stopExclusive,
						false);
				} else {
					return new ArrayIndexIterable(orderedArray, arrayIndex.startInclusive, arrayIndex.stopExclusive,
						true);
				}
			}

		} else {
			return new ArrayIndexIterable(orderedArray, 0, orderedArray.length, true);
		}

	}

	private static Comparator<Statement> getStatementComparator() {
		return (a, b) -> {
			int compare = valueComparator.compare(a.getPredicate(), b.getPredicate());
			if (compare != 0) return compare;

			compare = valueComparator.compare(a.getSubject(), b.getSubject());
			if (compare != 0) return compare;


			compare = valueComparator.compare(a.getObject(), b.getObject());
			return compare;
		};
	}
}

class SpoCompound {
	private Resource subject;
	private IRI predicate;
	private Value object;

	SpoCompound(Resource subject, IRI predicate, Value object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		SpoCompound that = (SpoCompound) o;
		return subject.equals(that.subject) &&
			predicate.equals(that.predicate) &&
			object.equals(that.object);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, predicate, object);
	}

	@Override
	public String toString() {
		return "SpoCompound{" +
			"subject=" + subject +
			", predicate=" + predicate +
			", object=" + object +
			'}';
	}
}

class SpCompound {
	private Resource subject;
	private IRI predicate;

	SpCompound(Resource subject, IRI predicate) {
		this.subject = subject;
		this.predicate = predicate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		SpCompound that = (SpCompound) o;
		return subject.equals(that.subject) &&
			predicate.equals(that.predicate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, predicate);
	}

	@Override
	public String toString() {
		return "SpCompound{" +
			"subject=" + subject +
			", predicate=" + predicate +
			'}';
	}
}

class SCompound {
	private Resource subject;

	SCompound(Resource subject) {
		this.subject = subject;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		SCompound sCompound = (SCompound) o;
		return subject.equals(sCompound.subject);
	}

	@Override
	public int hashCode() {
		return subject.hashCode();
	}

	@Override
	public String toString() {
		return "SCompound{" +
			"subject=" + subject +
			'}';
	}
}

class ArrayIndex {
	int startInclusive;
	int stopExclusive;

	ArrayIndex(int startInclusive, int stopExclusive) {
		this.startInclusive = startInclusive;
		this.stopExclusive = stopExclusive;
	}
}