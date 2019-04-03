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
class OrderedPSOIndex {

	private static final ArrayIndexIterable.EmptyArrayIndexIterable EMPTY_ARRAY_INDEX_ITERABLE = new ArrayIndexIterable.EmptyArrayIndexIterable();
	private final Statement[] orderedArray;

	private final Map<PsoCompound, ArrayIndex> psoIndex;
	private final Map<PsCompound, ArrayIndex> psIndex;
	private final Map<PCompound, ArrayIndex> pIndex;

	private static final ValueComparator valueComparator = new ValueComparator();


	OrderedPSOIndex(Set<Statement> statementSet) {
		this(statementSet
			.stream()
			.sorted(getStatementComparator())
			.toArray(Statement[]::new), true);
	}

	OrderedPSOIndex(Statement[] sortableStatements, boolean sorted) {

		psoIndex = new HashMap<>(0);
		psIndex = new HashMap<>(0);
		pIndex = new HashMap<>(sortableStatements.length / 5, 0.5f);

		if(!sorted){
			sortableStatements = Arrays
				.stream(sortableStatements)
				.sorted(getStatementComparator())
				.toArray(Statement[]::new);
		}

		orderedArray = sortableStatements;

		for (int i = 0; i < orderedArray.length; i++) {
			Statement statement = orderedArray[i];

			int index = i;

			PCompound pKey = new PCompound(statement.getPredicate());
			PsCompound psKey = new PsCompound(statement.getPredicate(), statement.getSubject());
			PsoCompound psoKey = new PsoCompound(statement.getPredicate(), statement.getSubject(), statement.getObject());

			pIndex.compute(pKey, (key, value) -> {
				if (value == null) {
					return new ArrayIndex(index, index + 1);
				} else {
					value.stopExclusive = index + 1;
					return value;
				}
			});

//			psIndex.compute(psKey, (key, value) -> {
//				if (value == null) {
//					return new ArrayIndex(index, index + 1);
//				} else {
//					value.stopExclusive = index + 1;
//					return value;
//				}
//			});
//
//			psoIndex.compute(psoKey, (key, value) -> {
//				if (value == null) {
//					return new ArrayIndex(index, index + 1);
//				} else {
//					value.stopExclusive = index + 1;
//					return value;
//				}
//			});

		}

	}

	ArrayIndexIterable getStatements(Resource subject, IRI predicate, Value object, Resource... context) {
		if (predicate != null) {
			if (subject != null) {
				if (object != null) {
					ArrayIndex arrayIndex = psoIndex.get(new PsoCompound(predicate, subject, object));
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
					ArrayIndex arrayIndex = psIndex.get(new PsCompound(predicate, subject));
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
				ArrayIndex arrayIndex = pIndex.get(new PCompound(predicate));
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

class PsoCompound {
	private IRI predicate;
	private Resource subject;
	private Value object;

	public PsoCompound(IRI predicate, Resource subject, Value object) {
		this.predicate = predicate;
		this.subject = subject;
		this.object = object;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		PsoCompound that = (PsoCompound) o;
		return predicate.equals(that.predicate) &&
			subject.equals(that.subject) &&
			object.equals(that.object);
	}

	@Override
	public int hashCode() {
		return Objects.hash(predicate, subject, object);
	}

	@Override
	public String toString() {
		return "PsoCompound{" +
			"predicate=" + predicate +
			", subject=" + subject +
			", object=" + object +
			'}';
	}
}

class PsCompound {
	private IRI predicate;
	private Resource subject;

	public PsCompound(IRI predicate, Resource subject) {
		this.predicate = predicate;
		this.subject = subject;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		PsCompound that = (PsCompound) o;
		return predicate.equals(that.predicate) &&
			subject.equals(that.subject);
	}

	@Override
	public int hashCode() {
		return Objects.hash(predicate, subject);
	}

	@Override
	public String toString() {
		return "PsCompound{" +
			"predicate=" + predicate +
			", subject=" + subject +
			'}';
	}
}

class PCompound {
	private IRI predicate;

	PCompound(IRI predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		PCompound pCompound = (PCompound) o;
		return predicate.equals(pCompound.predicate);
	}

	@Override
	public int hashCode() {
		return predicate.hashCode();
	}

	@Override
	public String toString() {
		return "PCompound{" +
			"predicate=" + predicate +
			'}';
	}
}

